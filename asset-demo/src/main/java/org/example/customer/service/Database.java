package org.example.customer.service;

import java.util.ArrayList;
import java.util.List;

public class Database {
    private static List<Customer> customers = new ArrayList<>();

    public static List<Customer> getCustomers() {
        return customers;
    }

    public static List<Customer> getCustomersDummy() {
        List<Customer> customers = new ArrayList<>();
        customers.add(new Customer(1, "Daniel", "Ornelas"));
        customers.add(new Customer(2, "Dennis", "McBride"));
        customers.add(new Customer(3, "Walter", "Wright"));
        customers.add(new Customer(4, "Mitchell", "Kish"));
        customers.add(new Customer(5, "Tracy", "Edwards"));
        return customers;
    }

    public static Customer getCustomer(int customerId) {
        for (Customer customer : customers) {
            if (customer.getId() == customerId)
                return customer;
        }

        return null;
    }

    public static void addCustomer(Customer customer) {
        customers.add(customer);
    }

    public static void deleteCustomer(int customerId) {
        for (Customer customer : customers) {
            if (customer.getId() == customerId) {
                customers.remove(customer);
                break;
            }
        }
    }
}