package win.doyto.query.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import win.doyto.query.config.GlobalConfiguration;
import win.doyto.query.core.test.TestEntity;
import win.doyto.query.core.test.TestEnum;
import win.doyto.query.core.test.TestQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CrudBuilderTest
 *
 * @author f0rb
 */
class CrudBuilderTest {

    private DynamicEntity dynamicEntity;
    private List<Object> argList;
    private CrudBuilder<TestEntity> userEntityCrudBuilder = new CrudBuilder<>(TestEntity.class);
    private CrudBuilder<DynamicEntity> dynamicEntityCrudBuilder = new CrudBuilder<>(DynamicEntity.class);

    @BeforeEach
    void setUp() {

        dynamicEntity = new DynamicEntity();
        dynamicEntity.setUser("f0rb");
        dynamicEntity.setProject("i18n");
        dynamicEntity.setScore(100);

        argList = new ArrayList<>();
    }

    @Test
    void create() {
        assertEquals("INSERT INTO user (username, password, mobile, email, nickname, userLevel, memo, valid) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                     userEntityCrudBuilder.buildCreateAndArgs(new TestEntity(), argList));
    }

    @Test
    void update() {
        assertEquals("UPDATE user SET username = ?, password = ?, mobile = ?, email = ?, nickname = ?, userLevel = ?, memo = ?, valid = ? WHERE id = ?",
                     userEntityCrudBuilder.buildUpdateAndArgs(new TestEntity(), argList));
    }

    @Test
    void createDynamicEntity() {

        assertEquals("INSERT INTO t_dynamic_f0rb_i18n (user_score, memo) VALUES (?, ?)",
                     dynamicEntityCrudBuilder.buildCreateAndArgs(dynamicEntity, argList));
        assertThat(argList).containsExactly(100, null);
    }

    @Test
    void updateDynamicEntity() {
        dynamicEntity.setId(1);

        assertEquals("UPDATE t_dynamic_f0rb_i18n SET user_score = ?, memo = ? WHERE id = ?",
                     dynamicEntityCrudBuilder.buildUpdateAndArgs(dynamicEntity, argList));
        assertThat(argList).containsExactly(100, null, 1);
    }


    @Test
    void buildPatchAndArgs() {
        dynamicEntity.setId(1);
        dynamicEntity.setScore(null);
        dynamicEntity.setMemo("memo");

        assertEquals("UPDATE t_dynamic_f0rb_i18n SET memo = ? WHERE id = ?",
                     dynamicEntityCrudBuilder.buildPatchAndArgsWithId(dynamicEntity, argList));
        assertThat(argList).containsExactly("memo", 1);
    }

    @Test
    void replaceTableName() {

        DynamicEntity entity = new DynamicEntity();
        entity.setId(1);
        entity.setUser("f0rb");
        entity.setProject("i18n");

        assertEquals("t_dynamic_f0rb_i18n", CommonUtil.replaceHolderInString(entity, DynamicEntity.TABLE));
        assertEquals("user", CommonUtil.replaceHolderInString(new TestEntity(), TestEntity.TABLE));

    }
    @Test
    void fixReplaceTableNameWithTail() {

        DynamicEntity entity = new DynamicEntity();
        entity.setId(1);
        entity.setUser("f0rb");
        entity.setProject("i18n");

        assertEquals("t_dynamic_f0rb_i18n_any", CommonUtil.replaceHolderInString(entity, "t_dynamic_${user}_${project}" + "_any"));

    }

    @Test
    public void supportMapFieldToUnderscore() {
        GlobalConfiguration.instance().setMapCamelCaseToUnderscore(true);

        try {
            TestEntity testEntity = new TestEntity();
            testEntity.setId(1);
            testEntity.setUserLevel(TestEnum.VIP);
            testEntity.setValid(true);
            assertEquals("UPDATE user SET user_level = ?, valid = ? WHERE id = ?",
                         userEntityCrudBuilder.buildPatchAndArgsWithId(testEntity, argList));
            assertThat(argList).containsExactly(0, true, 1);
        } finally {
            GlobalConfiguration.instance().setMapCamelCaseToUnderscore(false);
        }
    }

    @Test
    public void buildPatchAndArgsWithQuery() {
        TestEntity testEntity = new TestEntity();
        testEntity.setNickname("测试");

        TestQuery testQuery = TestQuery.builder().username("test").build();

        assertEquals("UPDATE user SET nickname = ? WHERE username = ?",
                     userEntityCrudBuilder.buildPatchAndArgsWithQuery(testEntity, testQuery, argList));
        assertThat(argList).containsExactly("测试", "test");
    }

    @Test
    void createMulti() {
        SqlAndArgs sqlAndArgs = userEntityCrudBuilder.buildCreateAndArgs(Arrays.asList(new TestEntity(), new TestEntity(), new TestEntity()));
        assertEquals(
            "INSERT INTO user (username, password, mobile, email, nickname, userLevel, memo, valid) VALUES " +
                "(?, ?, ?, ?, ?, ?, ?, ?), (?, ?, ?, ?, ?, ?, ?, ?), (?, ?, ?, ?, ?, ?, ?, ?)", sqlAndArgs.sql);
    }

    @Test
    public void supportDynamicTableName() {
        DynamicQuery dynamicQuery = DynamicQuery.builder().user("f0rb").project("i18n").scoreLt(100).build();

        assertEquals("SELECT * FROM t_dynamic_f0rb_i18n WHERE score < ?",
                     dynamicEntityCrudBuilder.buildSelectAndArgs(dynamicQuery, argList));
        assertThat(argList).containsExactly(100);
    }

    @Test
    public void fixSQLInject() {
        DynamicQuery dynamicQuery = DynamicQuery.builder().user("f0rb").project("; DROP TABLE menu;").scoreLt(100).build();

        assertEquals("SELECT * FROM t_dynamic_f0rb_${project} WHERE score < ?",
                     dynamicEntityCrudBuilder.buildSelectAndArgs(dynamicQuery, argList));
        assertThat(argList).containsExactly(100);
    }

    @Test
    public void buildDeleteAndArgs() {
        TestQuery testQuery = TestQuery.builder().username("test").build();
        testQuery.setPageNumber(3).setPageSize(10);
        assertEquals("DELETE FROM user WHERE username = ? LIMIT 10",
                     userEntityCrudBuilder.buildDeleteAndArgs(testQuery, argList));
        assertEquals(1, argList.size());
        assertEquals("test", argList.get(0));
    }

    @Test
    public void supportUnderlineScore() {
        DynamicQuery dynamicQuery = DynamicQuery.builder().user("f0rb").project("i18n_0001").scoreLt(100).build();

        assertEquals("SELECT * FROM t_dynamic_f0rb_i18n_0001 WHERE score < ?",
                     dynamicEntityCrudBuilder.buildSelectAndArgs(dynamicQuery, argList));
        assertThat(argList).containsExactly(100);
    }

    @Test
    void createMultiOnDuplicate() {
        SqlAndArgs sqlAndArgs = userEntityCrudBuilder.buildCreateAndArgs(
            Arrays.asList(new TestEntity(), new TestEntity(), new TestEntity()),
            "mobile", "email"
        );
        assertEquals(
            "INSERT INTO user (username, password, mobile, email, nickname, userLevel, memo, valid) VALUES " +
                "(?, ?, ?, ?, ?, ?, ?, ?), (?, ?, ?, ?, ?, ?, ?, ?), (?, ?, ?, ?, ?, ?, ?, ?)" +
                " ON DUPLICATE KEY UPDATE " +
                "mobile = VALUES (mobile), " +
                "email = VALUES (email)",
            sqlAndArgs.sql);
    }

}