# OSS 签名URL动态生成改造说明

## 📋 改造概述

将原来存储**签名URL**的方式改为存储**object_key**，前端每次加载时动态生成签名URL，彻底解决URL过期问题。

---

## 🎯 改造内容

### 1. 数据库改造

#### 新增字段
- **object_key** (VARCHAR 512): 存储OSS对象键（文件路径）
- 原 **url** 字段保留但不再用于存储签名URL

#### 索引调整
- 删除 `idx_images_url` 唯一索引
- 新增 `idx_images_object_key` 唯一索引

#### 迁移脚本
执行文件：`diary-common/src/main/java/diary/common/entity/image/migration_add_object_key.sql`

```sql
-- 1. 添加 object_key 字段
ALTER TABLE image ADD COLUMN object_key VARCHAR(512) NOT NULL DEFAULT '' COMMENT 'OSS对象键';

-- 2. 复制现有数据到 object_key
UPDATE image SET object_key = url WHERE object_key = '';

-- 3. 调整索引
ALTER TABLE image DROP INDEX idx_images_url;
ALTER TABLE image ADD UNIQUE INDEX idx_images_object_key (object_key);
```

---

### 2. 后端代码改造

#### 2.1 实体类修改

**ImagePO.java**
```java
private String url;          // 图片访问URL（签名URL，前端展示用）
private String objectKey;    // OSS对象键（文件路径，用于生成签名URL）
```

**OssUploadSuccessMsg.java**
```java
private String objectKey;    // 从 ossUrl 改为 objectKey
```

#### 2.2 上传逻辑修改

**AsyncServiceImpl.java** - 图片上传
- ❌ 删除：生成签名URL的代码
- ✅ 改为：直接存储 fileName（即 object_key）

```java
// 上传到OSS
ossClient.putObject(bucketName, fileName, file.getInputStream());

// 存储 object_key 而非签名URL
OssUploadSuccessMsg msg = new OssUploadSuccessMsg(
    imageId, fileName, file.getOriginalFilename(), System.currentTimeMillis()
);
```

**VideoFileServiceImpl.java** - 视频上传
- 同样修改 simpleUpload 和 multipartUpload 方法
- 返回 fileName 而非签名URL

#### 2.3 消息队列消费者修改

**MqConsumerServiceImpl.java**
```java
String objectKey = message.getObjectKey();  // 从 getObjectUrl 改为 getObjectKey
imageMapper.updateImageStatusById(id, objectKey, status);
```

#### 2.4 Mapper XML 修改

**ImageMapper.xml**
```xml
<!-- 查询时返回 object_key -->
<select id="selectImagesByIds" resultMap="ImageResultMap">
    SELECT id, object_key FROM image WHERE id IN (...)
</select>

<!-- 更新时存储 object_key -->
<update id="updateImageStatusById">
    update image set object_key = #{ossUrl}, status = #{status} where id = #{id}
</update>
```

#### 2.5 动态生成签名URL

**OssUtil.java** - 新增批量生成方法
```java
public Map<String, String> batchGenerateSignedUrls(List<String> objectKeys) {
    Map<String, String> urlMap = new HashMap<>();
    for (String objectKey : objectKeys) {
        String signedUrl = generateSignedUrl(objectKey);  // 5分钟有效期
        urlMap.put(objectKey, signedUrl);
    }
    return urlMap;
}
```

**QueryUrlServiceImpl.java** - 查询时动态生成
```java
// 1. 从数据库查询 object_key
List<ImagePO> imagePOS = imageMapper.selectImagesByIds(imageIds);

// 2. 提取所有 object_key
List<String> objectKeys = imagePOS.stream()
    .map(ImagePO::getObjectKey)
    .toList();

// 3. 批量生成签名 URL
Map<String, String> signedUrlMap = ossUtil.batchGenerateSignedUrls(objectKeys);

// 4. 构建返回结果
return imagePOS.stream().map(imagePO -> {
    ImageVO imageVO = new ImageVO();
    imageVO.setId(imagePO.getId());
    imageVO.setUrl(signedUrlMap.get(imagePO.getObjectKey()));
    return imageVO;
}).toList();
```

---

## 🔄 前端调用方式

### 现有接口（已改造）
```
POST /file/query/images/urls
```

**请求参数**
```json
[123456, 123457, 123458]
```

**响应数据**
```json
{
  "code": 200,
  "data": [
    {
      "id": 123456,
      "url": "https://bucket.oss-cn-chengdu.aliyuncs.com/diary/image/xxx.jpg?Expires=xxx&Signature=xxx"
    },
    {
      "id": 123457,
      "url": "https://bucket.oss-cn-chengdu.aliyuncs.com/diary/image/yyy.jpg?Expires=xxx&Signature=xxx"
    }
  ]
}
```

### 前端使用建议

```javascript
// ❌ 错误做法：缓存签名URL长期使用
const imageUrl = await fetchSignedUrl(imageId);
// 5分钟后URL过期，图片无法显示

// ✅ 正确做法：每次加载日记时重新获取签名URL
async function loadDiary(diaryId) {
  const diary = await fetchDiary(diaryId);
  const imageIds = diary.images.map(img => img.id);
  
  // 每次加载都获取最新的签名URL
  const signedUrls = await fetchSignedUrls(imageIds);
  
  // 使用最新的URL渲染图片
  diary.images.forEach(img => {
    const signedUrl = signedUrls.find(u => u.id === img.id);
    img.url = signedUrl.url;
  });
  
  return diary;
}
```

---

## ⚙️ 签名URL有效期

当前配置：**5分钟**

修改位置：`diary-utils/src/main/java/diary/utils/OSS/OssUtil.java`

```java
private String generateSignedUrl(String key) {
    // 修改这里的 5 为你需要的分钟数
    Date expiration = new Date(System.currentTimeMillis() + 5 * 60 * 1000);
    ...
}
```

---

## ✅ 改造优势

1. **永久可用**：URL过期不影响使用，每次都生成新的
2. **安全可控**：签名URL短期有效，泄露风险低
3. **权限管理**：可根据用户权限动态控制是否生成签名URL
4. **灵活配置**：可随时调整签名URL有效期

---

## 🚨 注意事项

### 数据库迁移
1. 先在测试环境执行迁移脚本
2. 确认现有url字段的数据格式
3. 如果url存储的是完整URL，需要提取object_key部分
4. 备份数据后再执行迁移

### 前端改造
1. 不能缓存签名URL
2. 每次加载日记/图片列表时都要调用接口获取最新URL
3. 建议在数据加载时统一获取，避免频繁请求

### 性能优化
- 已实现批量生成签名URL，避免循环调用
- 5分钟有效期平衡了安全性和性能

---

## 📝 改造文件清单

### 数据库
- ✅ `diary-common/.../image/image.sql` - 表结构定义
- ✅ `diary-common/.../image/migration_add_object_key.sql` - 迁移脚本

### 实体类
- ✅ `diary-common/.../image/po/ImagePO.java`
- ✅ `diary-common/.../file/po/OssUploadSuccessMsg.java`

### Service层
- ✅ `diary-file/.../asyncserviceImpl/AsyncServiceImpl.java`
- ✅ `diary-file/.../VideoFileServiceImpl.java`
- ✅ `diary-file/.../MqConsumerServiceImpl.java`
- ✅ `diary-file/.../queryurlserviceImpl/QueryUrlServiceImpl.java`

### 工具类
- ✅ `diary-utils/.../OSS/OssUtil.java`

### Mapper
- ✅ `diary-file/.../mapper/ImageMapper.xml`

### Controller
- ✅ `diary-file/.../controller/FileController.java`

---

## 🎉 改造完成

所有改造已完成，无编译错误。按照以下步骤部署：

1. **执行数据库迁移脚本**
2. **部署后端服务**
3. **前端调整调用逻辑**（每次加载获取最新URL）
4. **测试验证**
