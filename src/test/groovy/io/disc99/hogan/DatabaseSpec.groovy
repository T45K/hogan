package io.disc99.hogan

import groovy.sql.Sql
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import java.sql.Timestamp
import java.sql.Date

class DatabaseSpec extends Specification {

    Sql sql
    Database db

    def setup() {
        sql = Sql.newInstance("jdbc:h2:mem:", "org.h2.Driver")
        db = new Database(sql)
        sql.execute '''
            |CREATE TABLE item_master (
            |  ID INT,
            |  NAME VARCHAR,
            |  PRICE INT);
            |CREATE TABLE sales (
            |  ID INT,
            |  DATE VARCHAR,
            |  ITEM_ID INT,
            |  COUNT INT,
            |  create_at DATE);
            |CREATE TABLE persons (
            |  ID INT,
            |  AGE NUMBER,
            |  NAME VARCHAR,
            |  DATE DATE,
            |  START TIMESTAMP);
            '''.stripMargin().toString()
    }

    def "insert data"() {
        when:
        db.insert {
            item_master:
            id | name     | price
            1  | 'Apple'  | 500
            2  | 'Orange' | 250

            sales:
            id | date         | item_id | count | create_at
            1  | '2015-04-01' | 1       | 3     | new Date(1)
            1  | '2015-04-02' | 2       | 1     | new Date(2)
            1  | '2015-04-02' | 1       | 2     | new Date(3)
        }

        then:
        sql.rows("select * from sales").toString() == '[' +
                '[ID:1, DATE:2015-04-01, ITEM_ID:1, COUNT:3, CREATE_AT:1970-01-01], ' +
                '[ID:1, DATE:2015-04-02, ITEM_ID:2, COUNT:1, CREATE_AT:1970-01-01], ' +
                '[ID:1, DATE:2015-04-02, ITEM_ID:1, COUNT:2, CREATE_AT:1970-01-01]]'

        sql.rows("select * from item_master").toString() == '[' +
                '[ID:1, NAME:Apple, PRICE:500], ' +
                '[ID:2, NAME:Orange, PRICE:250]]'
    }

    @Ignore("sql has bug")
    def "assert table"() {
        setup:
        sql.dataSet("item_master").add(id: 100, name: 'Banana')
        sql.dataSet("item_master").add(id: 101, name: 'Pineapple')
        sql.dataSet("sales").add(id: 1, count: 10, create_at: new Date(Long.MAX_VALUE))
        sql.dataSet("sales").add(id: 2, count: 10, create_at: new Date(0))
        sql.dataSet("sales").add(id: 3, count: 50, create_at: new Date(Long.MAX_VALUE))

        expect:
        db.assert {
            item_master:
            id  | name
            100 | 'Banana'
            101 | 'Pineapple'

            sales: 'id = 3 or create_at < current_date()'
            id | count
            2  | 10
            3  | 50
        }
    }

    def "multi format"() {
        when:
        db.insert {
            persons:
            id | age | name  | date        | start
            1  | 2   | 'tom' | new Date(1) | Timestamp.valueOf("1970-01-01 00:00:00.001")
        }

        then:
        sql.rows("select * from persons").toString() == '[[ID:1, AGE:2, NAME:tom, DATE:1970-01-01, START:1970-01-01 00:00:00.001]]'
    }

    @Unroll
    def "Check delegate SQL logic"() {
        when:
        logic.call()

        then:
        notThrown(Exception)

        where:
        logic << [
                { new Database(sql()) },
                { new Database(sql().getConnection()) },
                { new Database("jdbc:h2:mem:") },
                { new Database("jdbc:h2:mem:", "org.h2.Driver") },
                { new Database("jdbc:h2:mem:", "sa", "") },
                { new Database("jdbc:h2:mem:", "sa", "", "org.h2.Driver") },
                { new Database([url: "jdbc:h2:mem:", user: "sa", password: "", driverClassName: "org.h2.Driver"]) },
                { new Database(sql()).getSql() },
                { new Database(sql()).commit() },
                { new Database(sql()).rollback() },
                { new Database(sql()).close() },
        ]
    }


    Sql sql() {
        Sql.newInstance("jdbc:h2:mem:", "org.h2.Driver")
    }
}
