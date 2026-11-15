package diary.file.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MapperXmlContractTest {

    @ParameterizedTest
    @MethodSource("mapperResources")
    void shouldDeclareStatementForEveryMapperMethod(Class<?> mapperType, String resource) throws IOException {
        Configuration configuration = new Configuration();
        configuration.addMapper(mapperType);

        try (InputStream input = Resources.getResourceAsStream(resource)) {
            XMLMapperBuilder parser = new XMLMapperBuilder(
                    input,
                    configuration,
                    resource,
                    configuration.getSqlFragments()
            );
            parser.parse();
        }

        for (Method method : mapperType.getDeclaredMethods()) {
            String statementId = mapperType.getName() + "." + method.getName();
            assertThat(configuration.hasStatement(statementId))
                    .as("MyBatis statement %s", statementId)
                    .isTrue();
        }
    }

    private static Stream<Arguments> mapperResources() {
        return Stream.of(
                Arguments.of(ImageMapper.class, "mapper/ImageMapper.xml"),
                Arguments.of(ThumbnailMapper.class, "mapper/ThumbnailMapper.xml"),
                Arguments.of(ImageUploadSessionMapper.class, "mapper/ImageUploadSessionMapper.xml"),
                Arguments.of(ImageUploadChunkMapper.class, "mapper/ImageUploadChunkMapper.xml")
        );
    }
}
