package diary.diarygoal.service.export;

/** 导出文件及其 HTTP 响应元数据。 */
public record GoalExportFile(byte[] content, String filename, String contentType) {
}
