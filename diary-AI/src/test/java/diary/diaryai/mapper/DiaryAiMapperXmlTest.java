package diary.diaryai.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class DiaryAiMapperXmlTest {
    @Test
    void mapperXmlIsValidAndContainsNewStateTransitions() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream input = Resources.getResourceAsStream("mapper/DiaryAIMapper.xml")) {
            new XMLMapperBuilder(input, configuration, "mapper/DiaryAIMapper.xml",
                    configuration.getSqlFragments()).parse();
        }

        assertThat(configuration.hasStatement(
                "diary.diaryai.mapper.DiaryAiMapper.recoverStaleWaiting")).isTrue();
        assertThat(configuration.hasStatement(
                "diary.diaryai.mapper.DiaryAiMapper.selectTimedOutbox")).isTrue();
        assertThat(configuration.hasStatement(
                "diary.diaryai.mapper.DiaryAiMapper.markQueuedByTaskIdIfWaiting")).isTrue();
        assertThat(configuration.hasStatement(
                "diary.diaryai.mapper.DiaryAiMapper.deleteExpiredSentOutbox")).isTrue();
    }
}
