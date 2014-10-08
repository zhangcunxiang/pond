package pond.db;

import pond.common.S;
import pond.common.sql.Criterion;

import java.util.List;

import static pond.common.S._for;

public class RecordServiceTest {

    public static void test2() {
        RecordService<TestRecord> recordService =
                RecordService.build(new TestRecord());
        List<TestRecord> list =
                recordService.query();
        S.echo(S.dump(_for(list).map(TestRecord::view).toList()));
    }


    public static void test4() {
        RecordService<TestRecord> recordService =
                RecordService.build(new TestRecord());
        List<TestRecord> list =
                recordService.query("create_time", Criterion.BETWEEN,
                        "1402230396671", "1402230396673"
                );
        S.echo(S.dump(_for(list).map(TestRecord::view).toList()));
    }

    public static void test5() {
        RecordService<TestRecord> recordService =
                RecordService.build(new TestRecord());
        List<TestRecord> list =
                recordService.query("create_time", Criterion.BETWEEN,
                        "1402230396671", "1402230396673",
                        "vid", Criterion.LIKE, "%02632%"
                );
        S.echo(S.dump(_for(list).map(TestRecord::view).toList()));
    }

    public static void test3() {
        RecordService<TestRecord> recordService =
                RecordService.build(new TestRecord());
        List<TestRecord> list =
                recordService.query("1 = 1 AND 1 > 0");
        S.echo(S.dump(_for(list).map(TestRecord::view).toList()));
    }

    public static void main(String[] args){
        test2();
        test3();
        test4();
        test5();
    }

}