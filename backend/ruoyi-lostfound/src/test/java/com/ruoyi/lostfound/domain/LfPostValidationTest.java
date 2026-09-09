package com.ruoyi.lostfound.domain;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class LfPostValidationTest
{
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeValidator()
    {
        FACTORY.close();
    }

    @Test
    void requiredFieldsMatchThePublishForm()
    {
        Set<String> fields = fields(new LfPost());
        assertTrue(fields.containsAll(Set.of("postType", "title", "itemCategoryId",
                "colorCategoryId", "campusCategoryId", "eventDate")));
    }

    @Test
    void completePostDoesNotRequireOptionalAreaOrDescription()
    {
        assertTrue(fields(validPost()).isEmpty());
    }

    @Test
    void rejectWhitespaceTitleFutureDateAndInvalidIds()
    {
        LfPost post = validPost();
        post.setTitle("   ");
        post.setEventDate(Date.valueOf(LocalDate.now().plusDays(1)));
        post.setItemCategoryId(-1L);
        post.setAreaCategoryId(0L);
        assertTrue(fields(post).containsAll(Set.of("title", "eventDate", "itemCategoryId", "areaCategoryId")));
    }

    @Test
    void rejectOverlongTitleAndUnknownPostType()
    {
        LfPost post = validPost();
        post.setTitle("x".repeat(129));
        post.setPostType("OTHER");
        assertTrue(fields(post).containsAll(Set.of("title", "postType")));
    }

    private Set<String> fields(LfPost post)
    {
        return VALIDATOR.validate(post).stream()
                .map(violation -> violation.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    private LfPost validPost()
    {
        LfPost post = new LfPost();
        post.setPostType("LOST");
        post.setTitle("Test post");
        post.setItemCategoryId(1L);
        post.setColorCategoryId(2L);
        post.setCampusCategoryId(3L);
        post.setEventDate(Date.valueOf(LocalDate.now()));
        return post;
    }
}
