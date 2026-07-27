package com.gonet.search.extract;

import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractMethod;
import kr.dogfoot.hwpxlib.tool.textextractor.TextMarks;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 파일 본문 텍스트 추출기 (DESIGN.md 4.4 파일 텍스트 추출 배치).
 *
 * - 허용 확장자는 application.yml `search.extract.extensions`에 선언된 것만 처리
 * - DOC/DOCX/XLS/XLSX/PPT/PPTX/PDF/TXT/CSV: Apache Tika (내부적으로 POI·PDFBox 사용)
 * - HWP: kr.dogfoot:hwplib / HWPX: kr.dogfoot:hwpxlib
 *   (요청받은 rhwp(github.com/edwardkim/rhwp)는 Rust+WASM/npm 라이브러리라 자바에서 사용 불가 — 동일 역할의 자바 라이브러리로 대체)
 */
@Component
public class FileTextExtractor {

    private static final int MAX_TEXT_LENGTH = 1_000_000;   // 추출 텍스트 상한 (1M chars)

    private final Set<String> allowedExtensions;
    private final Tika tika;

    public FileTextExtractor(@Value("${search.extract.extensions}") String extensions) {
        this.allowedExtensions = Arrays.stream(extensions.split(","))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        this.tika = new Tika();
        this.tika.setMaxStringLength(MAX_TEXT_LENGTH);
    }

    /** yml에 선언된 확장자인지 (대소문자 무시) */
    public boolean supports(String extension) {
        return extension != null && allowedExtensions.contains(extension.toUpperCase(Locale.ROOT));
    }

    public Set<String> allowedExtensions() {
        return allowedExtensions;
    }

    /** 파일 본문 텍스트 추출. 미선언 확장자는 IllegalArgumentException. */
    public String extract(File file, String extension) throws Exception {
        String ext = extension == null ? "" : extension.toUpperCase(Locale.ROOT);
        if (!supports(ext)) {
            throw new IllegalArgumentException("선언되지 않은 확장자: " + extension);
        }
        return switch (ext) {
            case "HWP" -> extractHwp(file);
            case "HWPX" -> extractHwpx(file);
            default -> tika.parseToString(file);            // DOC/DOCX/XLS/XLSX/PPT/PPTX/PDF/TXT/CSV
        };
    }

    private String extractHwp(File file) throws Exception {
        HWPFile hwp = HWPReader.fromFile(file);
        String text = kr.dogfoot.hwplib.tool.textextractor.TextExtractor.extract(
                hwp, kr.dogfoot.hwplib.tool.textextractor.TextExtractMethod.InsertControlTextBetweenParagraphText);
        return limit(text);
    }

    private String extractHwpx(File file) throws Exception {
        HWPXFile hwpx = HWPXReader.fromFile(file);
        String text = kr.dogfoot.hwpxlib.tool.textextractor.TextExtractor.extract(
                hwpx, TextExtractMethod.InsertControlTextBetweenParagraphText, false, new TextMarks());
        return limit(text);
    }

    private String limit(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) : text;
    }
}
