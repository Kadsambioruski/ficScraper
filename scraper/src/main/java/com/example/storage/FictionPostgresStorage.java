package com.example.storage;

import com.example.model.Fiction;
import com.example.scrape.Site;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FictionPostgresStorage implements FictionStorage {

    private final Database database;

    public FictionPostgresStorage(Database database) {
        this.database = database;
    }

    @Override
    public void save(Fiction fiction) {
        String sql = """
            INSERT INTO fictions (
                id,
                fic_link,
                site,
                title,
                author,
                chap_amount,
                reader_progress,
                word_count,
                description,
                finished
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                fic_link = EXCLUDED.fic_link,
                site = EXCLUDED.site,
                title = EXCLUDED.title,
                author = EXCLUDED.author,
                chap_amount = EXCLUDED.chap_amount,
                reader_progress = EXCLUDED.reader_progress,
                word_count = EXCLUDED.word_count,
                description = EXCLUDED.description,
                finished = EXCLUDED.finished
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setObject(1, fiction.getId());
            statement.setString(2, fiction.getFicLink());
            statement.setString(3, fiction.getSite().name());
            statement.setString(4, fiction.getTitle());
            statement.setString(5, fiction.getAuthor());
            statement.setInt(6, fiction.getChapAmount());
            statement.setInt(7, fiction.getReaderProgress());
            statement.setLong(8, fiction.getWordCount());
            statement.setString(9, fiction.getDescription());
            statement.setBoolean(10, fiction.isFinished());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to save fiction: " + fiction.getId(),
                    e
            );
        }
    }

    @Override
    public Fiction get(UUID id) {
        String sql = """
            SELECT
                id,
                fic_link,
                site,
                title,
                author,
                chap_amount,
                reader_progress,
                word_count,
                description,
                finished
            FROM fictions
            WHERE id = ?
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return mapFiction(result);
                }
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to get fiction: " + id,
                    e
            );
        }
    }

    @Override
    public List<Fiction> getAll() {
        String sql = """
            SELECT
                id,
                fic_link,
                site,
                title,
                author,
                chap_amount,
                reader_progress,
                word_count,
                description,
                finished
            FROM fictions
            WHERE finished = FALSE
            ORDER BY title
            """;

        return getList(sql);
    }

    @Override
    public List<Fiction> getFinished() {
        String sql = """
            SELECT
                id,
                fic_link,
                site,
                title,
                author,
                chap_amount,
                reader_progress,
                word_count,
                description,
                finished
            FROM fictions
            WHERE finished = TRUE
            ORDER BY title
            """;

        return getList(sql);
    }

    @Override
    public void delete(UUID id) {
        String sql = """
            DELETE FROM fictions
            WHERE id = ?
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setObject(1, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to delete fiction: " + id,
                    e
            );
        }
    }

    @Override
    public void setFinished(UUID id, boolean finished) {
        String sql = """
            UPDATE fictions
            SET finished = ?
            WHERE id = ?
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setBoolean(1, finished);
            statement.setObject(2, id);

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to update finished state for fiction: " + id,
                    e
            );
        }
    }

    @Override
    public void setReaderProgress(UUID id, int chapter) {
        String sql = """
            UPDATE fictions
            SET reader_progress = ?
            WHERE id = ?
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, chapter);
            statement.setObject(2, id);

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to update reader progress for fiction: " + id,
                    e
            );
        }
    }

    @Override
    public void setChapterAmount(UUID id, int chapterAmount) {
        String sql = """
            UPDATE fictions
            SET chap_amount = GREATEST(chap_amount, ?)
            WHERE id = ?
            """;

        try (Connection connection = database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, chapterAmount);
            statement.setObject(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to update chapter amount for fiction: " + id, e);
        }
    }

    private List<Fiction> getList(String sql) {
        List<Fiction> fictions = new ArrayList<>();

        try (Connection connection = database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                fictions.add(mapFiction(result));
            }

            return fictions;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to get fictions",
                    e
            );
        }
    }

    private Fiction mapFiction(ResultSet result) throws SQLException {
        Fiction fiction = new Fiction();

        fiction.setId(
                result.getObject("id", UUID.class)
        );

        fiction.setFicLink(
                result.getString("fic_link")
        );

        fiction.setSite(
                Site.valueOf(result.getString("site"))
        );

        fiction.setTitle(
                result.getString("title")
        );

        fiction.setAuthor(
                result.getString("author")
        );

        fiction.setChapAmount(
                result.getInt("chap_amount")
        );

        fiction.setReaderProgress(
                result.getInt("reader_progress")
        );

        fiction.setWordCount(
                result.getInt("word_count")
        );

        fiction.setDescription(
                result.getString("description")
        );

        fiction.setFinished(
                result.getBoolean("finished")
        );

        return fiction;
    }
}