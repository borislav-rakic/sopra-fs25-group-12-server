package ch.uzh.ifi.hase.soprafs24.initializer;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;

@Component
public class DatabaseSeeder implements ApplicationRunner {

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        runSqlScript("sql/insert_ai_users.sql");
        // runSqlScript("sql/insert_test_users.sql");
    }

    private void runSqlScript(String path) throws Exception {
        ScriptUtils.executeSqlScript(dataSource.getConnection(), new ClassPathResource(path));
    }
}
