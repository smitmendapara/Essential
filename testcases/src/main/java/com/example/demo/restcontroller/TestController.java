package com.example.demo.restcontroller;

import com.example.demo.model.Employee;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by smit on 21/3/22.
 */

@RestController
public class TestController
{
    @GetMapping("/")
    public Employee getData()
    {
        Employee employee = new Employee();

        employee.setEmpId("1");

        employee.setDesignation("manager");

        employee.setName("employee1");

        employee.setSalary(2000);

        return employee;
    }
}
