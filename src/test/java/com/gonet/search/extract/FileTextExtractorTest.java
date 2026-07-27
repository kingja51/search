package com.gonet.search.extract;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 파일 텍스트 추출기 검증 — DB 없이 실행 (Tika 로컬 파싱) */
class FileTextExtractorTest {

    private final FileTextExtractor extractor =
            new FileTextExtractor("DOC, DOCX, XLS, XLSX, PPT, PPTX, PDF, TXT, CSV, HWP, HWPX");

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("선언된 확장자만 지원한다 (대소문자 무시)")
    void supportsDeclaredExtensionsOnly() {
        assertThat(extractor.supports("pdf")).isTrue();
        assertThat(extractor.supports("HWP")).isTrue();
        assertThat(extractor.supports("txt")).isTrue();
        assertThat(extractor.supports("exe")).isFalse();
        assertThat(extractor.supports("zip")).isFalse();
        assertThat(extractor.supports(null)).isFalse();
    }

    @Test
    @DisplayName("TXT 파일 본문을 추출한다")
    void extractTxt() throws Exception {
        Path file = tempDir.resolve("notice.txt");
        Files.writeString(file, "검색엔진 파일 추출 테스트 본문입니다.", StandardCharsets.UTF_8);

        String text = extractor.extract(file.toFile(), "TXT");

        assertThat(text).contains("검색엔진 파일 추출 테스트 본문입니다.");
    }

    @Test
    @DisplayName("CSV 파일 본문을 추출한다")
    void extractCsv() throws Exception {
        Path file = tempDir.resolve("data.csv");
        Files.writeString(file, "이름,부서\n홍길동,개발팀\n", StandardCharsets.UTF_8);

        String text = extractor.extract(file.toFile(), "CSV");

        assertThat(text).contains("홍길동").contains("개발팀");
    }

    @Test
    @DisplayName("HWPX 파일 본문을 추출한다 (hwpxlib 생성 → 추출 라운드트립)")
    void extractHwpx() throws Exception {
        kr.dogfoot.hwpxlib.object.HWPXFile hwpx =
                kr.dogfoot.hwpxlib.tool.blankfilemaker.BlankFileMaker.make();
        hwpx.sectionXMLFileList().get(0).paras().iterator().next()
                .addNewRun().addNewT().addText("한글 HWPX 추출 테스트 문서입니다");
        Path file = tempDir.resolve("sample.hwpx");
        kr.dogfoot.hwpxlib.writer.HWPXWriter.toFilepath(hwpx, file.toString());

        String text = extractor.extract(file.toFile(), "HWPX");

        assertThat(text).contains("한글 HWPX 추출 테스트 문서입니다");
    }

    @Test
    @DisplayName("HWP 파일 본문을 추출한다 (hwplib 생성 → 추출 라운드트립)")
    void extractHwp() throws Exception {
        kr.dogfoot.hwplib.object.HWPFile hwp =
                kr.dogfoot.hwplib.tool.blankfilemaker.BlankFileMaker.make();
        kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph paragraph =
                hwp.getBodyText().getSectionList().get(0).getParagraph(0);
        paragraph.createText();
        paragraph.getText().addString("한글 HWP 추출 테스트 문서입니다");
        Path file = tempDir.resolve("sample.hwp");
        kr.dogfoot.hwplib.writer.HWPWriter.toFile(hwp, file.toString());

        String text = extractor.extract(file.toFile(), "HWP");

        assertThat(text).contains("한글 HWP 추출 테스트 문서입니다");
    }

    @Test
    @DisplayName("선언되지 않은 확장자는 예외를 던진다")
    void rejectsUndeclaredExtension() {
        File file = tempDir.resolve("run.exe").toFile();

        assertThatThrownBy(() -> extractor.extract(file, "EXE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("선언되지 않은 확장자");
    }
}
