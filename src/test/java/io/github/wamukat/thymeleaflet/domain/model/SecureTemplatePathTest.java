package io.github.wamukat.thymeleaflet.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SecureTemplatePath のテスト
 * templatePath.replace 問題の解決を検証
 */
class SecureTemplatePathTest {

    @Test
    void testCreateUnsafe_shouldCreateSecureTemplatePath() {
        // Given
        String trustedPath = "domain.point.molecules";
        
        // When
        SecureTemplatePath securePath = SecureTemplatePath.createUnsafe(trustedPath);
        
        // Then
        assertNotNull(securePath);
        assertEquals("domain.point.molecules", securePath.forUrl());
        assertEquals("domain/point/molecules", securePath.forFilePath());
        assertEquals("domain/point/molecules", securePath.forDisplay());
        assertEquals("domain.point.molecules", securePath.forThymeleaf());
    }

    @Test
    void testFromNormalizedPath_shouldCreateFromSlashSeparated() {
        // Given
        String normalizedPath = "domain/point/molecules";
        
        // When
        SecureTemplatePath securePath = SecureTemplatePath.fromNormalizedPath(normalizedPath);
        
        // Then
        assertEquals("domain.point.molecules", securePath.forUrl());
        assertEquals("domain/point/molecules", securePath.forFilePath());
    }

    @Test
    void testForClasspath_shouldGenerateCorrectClasspathResource() {
        // Given
        SecureTemplatePath securePath = SecureTemplatePath.createUnsafe("shared.atoms.button");
        
        // When
        String classpathResource = securePath.forClasspath(".html");
        
        // Then
        assertEquals("classpath:templates/shared/atoms/button.html", classpathResource);
    }

    @Test
    void testPathDepth_shouldCalculateCorrectDepth() {
        // Given
        SecureTemplatePath shallow = SecureTemplatePath.createUnsafe("button");
        SecureTemplatePath deep = SecureTemplatePath.createUnsafe("domain.point.molecules");
        
        // Then
        assertEquals(1, shallow.getDepth());
        assertEquals(3, deep.getDepth());
    }

    @Test
    void testRootAndLastSegment_shouldExtractCorrectly() {
        // Given
        SecureTemplatePath securePath = SecureTemplatePath.createUnsafe("domain.point.molecules");
        
        // Then
        assertEquals("domain", securePath.getRootSegment());
        assertEquals("molecules", securePath.getLastSegment());
    }

    @Test
    void testStartsWith_shouldDetectPrefix() {
        // Given
        SecureTemplatePath securePath = SecureTemplatePath.createUnsafe("domain.point.molecules");
        
        // Then
        assertTrue(securePath.startsWith("domain"));
        assertFalse(securePath.startsWith("shared"));
    }
}