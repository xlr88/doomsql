package com.manish.doomsql

import com.manish.doomsql.data.engine.SqlFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SqlFormatterTest {

    @Test
    fun formatSimpleQuery() {
        val input = "select id, name, salary from employees where salary > 50000 order by salary desc"
        val formatted = SqlFormatter.format(input)

        assertTrue(formatted.contains("SELECT"))
        assertTrue(formatted.contains("FROM"))
        assertTrue(formatted.contains("WHERE"))
        assertTrue(formatted.contains("ORDER BY"))
        // Check that SELECT and FROM are on separate lines
        assertTrue(formatted.lines().size >= 3)
    }

    @Test
    fun formatWithJoins() {
        val input = "select e.name, d.department_name from employees e left join departments d on e.dept_id = d.id"
        val formatted = SqlFormatter.format(input)

        assertTrue(formatted.contains("LEFT JOIN"))
        assertTrue(formatted.contains("ON"))
    }

    @Test
    fun formatEmpty() {
        assertEquals("", SqlFormatter.format(""))
        assertEquals("", SqlFormatter.format("   "))
    }
}
