package com.elevenquest.jdbctest;

import java.io.Console;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        App app = new App();
        app.batchTest();
    }

    public App() {

    }

    static String longjson = """
            {
                "attr1" : {
                    "iattr1" : "ivalue1",
                    "iattr2" : "ivalue2",
                    "iattr3" : "ivalue3",
                    "iattr4" : "ivalue4",
                    "iattr5" : "ivalue5",
                    "iattr6" : "ivalue6",
                    "iattr7" : "ivalue7",
                    "iattr8" : "ivalue8"
                },
                "attr2" : "value2"
            }
            """;

    private void initialize() {
        try (Connection con = DataSource.getConnection();) {
            PreparedStatement pstmt = con.prepareStatement(
                    """
                            CREATE TABLE `git_file_changes` (
                              `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
                              `event` json DEFAULT NULL,
                              `git_uuid` varchar(255) NOT NULL DEFAULT '',
                              `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                              PRIMARY KEY (`id`)
                            ) ENGINE=InnoDB;""");
            pstmt.executeUpdate();
            con.commit();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
    }

    public void test() {
        try (Connection con = DataSource.getConnection();) {
            PreparedStatement pstmt = con
                    .prepareStatement("insert into git_file_changes (event, git_uuid) values(?, ?)");
            pstmt.setString(1, longjson);
            pstmt.setString(2, "2039402-92930203-04910230");
            pstmt.executeUpdate();
            totalCount.incrementAndGet();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
    }

    boolean isExist = false;
    AtomicLong totalCount = new AtomicLong();

    static class ExitLock {
        public static int count() {
            return locks.size();
        }

        static Vector<ExitLock> locks = new Vector<ExitLock>();
        private boolean needExit;

        private ExitLock() {
            needExit = false;
        }

        public void requestToExit() {
            this.needExit = true;
        }

        public boolean needExit() {
            return needExit;
        }

        public void finished() {
            locks.remove(this);
        }

        public static ExitLock newLock() {
            ExitLock lock = new ExitLock();
            locks.add(lock);
            return lock;
        }

        public static void exitRequest(int threadCount) {
            for (int i = 0; i < threadCount; i++) {
                locks.get(i).requestToExit();
            }
        }
    }

    public void newInsertThread() {
        final ExitLock lock = ExitLock.newLock();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!lock.needExit()) {
                    test();
                }
                lock.finished();
            }
        }).start();
    }

    public void printTransactionCount() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long prevTotalCount = totalCount.get();
                try {
                    Terminal terminal = TerminalBuilder.terminal();
                    while (!isExist) {
                        Thread.sleep(1000);
                        terminal.puts(InfoCmp.Capability.cursor_address, 0, 0);
                        System.out.println(String.format("%d %d", ExitLock.count(), (totalCount.get() - prevTotalCount)));
                        prevTotalCount = totalCount.get();
                    }
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }).start();
    }

    public void batchTest() {
        Console console = System.console();
        printTransactionCount();
        try {
            while (true) {
                String command = console.readLine("Input command(number - make # of thread, 0 - exit :");
                int numOfThread = Integer.parseInt(command);
                if (numOfThread == 0)
                    break;
                if (numOfThread < ExitLock.count()) {
                    ExitLock.exitRequest(ExitLock.count() - numOfThread);
                } else if (numOfThread > ExitLock.count()) {
                    int increaseTarget = numOfThread - ExitLock.count();
                    for (int i = 0; i < increaseTarget; i++) {
                        newInsertThread();
                    }
                }
            }
        } finally {
            ExitLock.exitRequest(ExitLock.count());
            isExist = true;
        }
    }

}
