package com.example.storage;

import com.example.model.Chapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChapterPostgresStorage implements ChapterStorage {

    private final Database database;

    public ChapterPostgresStorage(Database database) {
        this.database = database;
    }

@Override
public void save(Chapter chapter) {
    String findByChapterIdSql = """
        SELECT id
        FROM chapters
        WHERE fiction_id = ?
        AND url LIKE ?
        """;

    String findByTitleSql = """
        SELECT id
        FROM chapters
        WHERE fiction_id = ?
        AND title = ?
        """;

    String updateSql = """
        UPDATE chapters
        SET title = ?,
            url = ?,
            content = ?
        WHERE id = ?
        """;

    String insertSql = """
        INSERT INTO chapters (
            id,
            fiction_id,
            chapter_number,
            title,
            url,
            content
        )
        VALUES (?, ?, ?, ?, ?, ?)
        """;

    try (Connection connection = database.getConnection()) {
        String chapterId = extractRoyalRoadChapterId(chapter.getUrl());

        UUID existingId = null;

        if (chapterId != null) {
            try (PreparedStatement statement =
                         connection.prepareStatement(findByChapterIdSql)) {

                statement.setObject(1, chapter.getFictionId());
                statement.setString(2, "%/chapter/" + chapterId + "/%");

                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        existingId =
                                result.getObject("id", UUID.class);
                    }
                }
            }
        }

        if (existingId == null) {
            try (PreparedStatement statement =
                         connection.prepareStatement(findByTitleSql)) {

                statement.setObject(1, chapter.getFictionId());
                statement.setString(2, chapter.getTitle());

                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        existingId =
                                result.getObject("id", UUID.class);
                    }
                }
            }
        }

        if (existingId != null) {
            try (PreparedStatement statement =
                         connection.prepareStatement(updateSql)) {

                statement.setString(1, chapter.getTitle());
                statement.setString(2, chapter.getUrl());
                statement.setString(3, chapter.getContent());
                statement.setObject(4, existingId);

                statement.executeUpdate();
            }

            chapter.setId(existingId);
            return;
        }

        try (PreparedStatement statement =
                     connection.prepareStatement(insertSql)) {

            statement.setObject(1, chapter.getId());
            statement.setObject(2, chapter.getFictionId());
            statement.setInt(3, chapter.getChapterNumber());
            statement.setString(4, chapter.getTitle());
            statement.setString(5, chapter.getUrl());
            statement.setString(6, chapter.getContent());

            statement.executeUpdate();
        }

    } catch (SQLException e) {
        throw new RuntimeException(
                "Failed to save chapter: " + chapter.getId(),
                e
        );
    }
}

private String extractRoyalRoadChapterId(String url) {
    if (url == null) {
        return null;
    }

    String marker = "/chapter/";
    int start = url.indexOf(marker);

    if (start == -1) {
        return null;
    }

    start += marker.length();

    int end = url.indexOf("/", start);

    if (end == -1) {
        end = url.length();
    }

    String chapterId = url.substring(start, end);

    if (chapterId.isBlank()) {
        return null;
    }

    return chapterId;
}

    @Override
    public Chapter get(UUID id) {
        String sql = """
            SELECT
                id,
                fiction_id,
                chapter_number,
                title,
                url,
                content
            FROM chapters
            WHERE id = ?
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return mapChapter(result);
                }
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to get chapter: " + id,
                    e
            );
        }
    }

    @Override
    public List<Chapter> getForFiction(UUID fictionId) {
        String sql = """
            SELECT
                id,
                fiction_id,
                chapter_number,
                title,
                url,
                content
            FROM chapters
            WHERE fiction_id = ?
            ORDER BY chapter_number
            """;

        List<Chapter> chapters = new ArrayList<>();

        try (Connection connection = database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setObject(1, fictionId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    chapters.add(mapChapter(result));
                }
            }

            return chapters;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to get chapters for fiction: "
                            + fictionId,
                    e
            );
        }
    }

    @Override
    public void delete(UUID id) {
        String sql = """
            DELETE FROM chapters
            WHERE id = ?
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setObject(1, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to delete chapter: " + id,
                    e
            );
        }
    }

    private Chapter mapChapter(ResultSet result) throws SQLException {
        Chapter chapter = new Chapter();

        chapter.setId(
                result.getObject("id", UUID.class)
        );

        chapter.setFictionId(
                result.getObject("fiction_id", UUID.class)
        );

        chapter.setChapterNumber(
                result.getInt("chapter_number")
        );

        chapter.setTitle(
                result.getString("title")
        );

        chapter.setUrl(
                result.getString("url")
        );

        chapter.setContent(
                result.getString("content")
        );

        return chapter;
    }
}