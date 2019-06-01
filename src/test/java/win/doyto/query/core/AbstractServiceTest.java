package win.doyto.query.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import win.doyto.query.core.test.TestEntity;
import win.doyto.query.core.test.TestQuery;
import win.doyto.query.core.test.TestService;
import win.doyto.query.entity.EntityAspect;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static win.doyto.query.core.test.TestEntity.initUserEntities;

/**
 * AbstractServiceTest
 *
 * @author f0rb
 */
class AbstractServiceTest {
    TestService testService;

    @BeforeEach
    void setUp() {
        testService = new TestService();
        testService.dataAccess = spy(testService.dataAccess);
        testService.batchInsert(initUserEntities());
    }

    @Test
    void supportCache() {
        testService.setCacheManager(new ConcurrentMapCacheManager());
        testService.get(1);
        testService.get(1);
        verify(testService.dataAccess, times(1)).get(1);
    }

    @Test
    void supportEvictCache() {
        testService.setCacheManager(new ConcurrentMapCacheManager());
        TestEntity testEntity = testService.get(1);
        testService.update(testEntity);
        testService.get(1);
        verify(testService.dataAccess, times(2)).get(1);
    }

    @Test
    void supportAspect() {
        EntityAspect<TestEntity> entityAspect = spy(new EntityAspect<TestEntity>() {
            @Override
            public void afterUpdate(TestEntity origin, TestEntity current) {
                assertNotSame(origin, current);
                assertEquals("test1", origin.getUsername());
                assertEquals("test2", current.getUsername());
            }
        });

        testService.entityAspects.add(entityAspect);
        TestEntity e = new TestEntity();
        e.setUsername("test1");
        testService.create(e);
        verify(entityAspect, times(1)).afterCreate(e);

        TestEntity u = new TestEntity();
        u.setId(e.getId());
        u.setUsername("test2");
        testService.update(u);
        verify(entityAspect, times(1)).afterUpdate(any(), eq(u));

        testService.delete(e.getId());
        verify(entityAspect, times(1)).afterDelete(u);
    }

    @Test
    void count() {
        assertEquals(1, testService.count(TestQuery.builder().username("username1").build()));
    }

    @Test
    void exists() {
        assertTrue(testService.exists(TestQuery.builder().username("username1").build()));
    }

    @Test
    void deleteByQuery() {
        TestQuery testQuery = TestQuery.builder().usernameLike("username").build();
        assertEquals(4, testService.delete(testQuery));
    }

    @Test
    void deleteByQueryWithLimit() {
        TestQuery testQuery = TestQuery.builder().usernameLike("username").build();
        testQuery.setPageSize(2);
        assertEquals(2, testService.delete(testQuery));
    }
}