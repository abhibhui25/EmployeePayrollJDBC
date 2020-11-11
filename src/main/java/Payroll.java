import java.sql.*;
import java.util.*;

public class Payroll {
    private Payroll() {
    }

    private static Payroll pinstance = null;

    public static Payroll getInstance() {
        if (pinstance == null)
            pinstance = new Payroll();

        return pinstance;
    }

    ConnectionRetriever con = new ConnectionRetriever();
    PreparedStatement payrollUpdateStatement;

    public List<Employee> readData() {
        String sql = "select * from employee e, payroll p where e.emp_id=p.emp_id ;";
        List<Employee> arr = new ArrayList<Employee>();
        try {
            Connection c = con.getConnection();
            Statement statement = c.createStatement();
            ResultSet result = statement.executeQuery(sql);
            while (result.next()) {
                Employee e = new Employee();
                e.company_id = result.getInt("company_id");
                e.emp_id = result.getInt("emp_id");
                e.name = result.getString("name");
                e.phone = result.getString("phone");
                e.address = result.getString("address");
                e.gender = result.getString("gender").charAt(0);
                e.basic_pay = result.getInt("basic_pay");
                arr.add(e);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return arr;
    }

    public void addEmployeesWithThread(ArrayList<Employee> employeeList) {
        System.out.println("addEmployeesWithThread");
        for (Employee e : employeeList) {
            Runnable task = () -> {
                createEmployee(e);
            };
            Thread thread = new Thread(task);
            thread.start();
            try {
                thread.sleep(150);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
    }

    public void updateSalaryWithThread(String[] name, int[] salary) {
        System.out.println("updateSalaryWithThread");
        for (int i = 0; i < name.length; i++) {
            String n = name[i];
            int s = salary[i];
            Runnable task = () -> {
                update(n, s);
            };
            Thread thread = new Thread(task);
            thread.start();
            try {
                thread.sleep(10000);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
    }

    public int getBetween(int start, int end) {
        int count = 0;
        Connection c = con.getConnection();
        try {
            payrollUpdateStatement = c.prepareStatement("select * from employee where emp_id between ? and ?");
            payrollUpdateStatement.setInt(1, start);
            payrollUpdateStatement.setInt(2, end);
            ResultSet result = payrollUpdateStatement.executeQuery();
            while (result.next()) {
                count++;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return count;
    }

    public void createEmployee(Employee e) {
        Connection c = con.getConnection();
        try {
            c.setAutoCommit(false);
            //Pushing Company details
            payrollUpdateStatement = c.prepareStatement("insert into company(company_name) values (?)");
            payrollUpdateStatement.setString(1, e.company_name);
            payrollUpdateStatement.executeUpdate();
            //Getting the company_id which is auto-set in sql and setting it into the object
            payrollUpdateStatement = c.prepareStatement("select company_id from company where company_name=?");
            payrollUpdateStatement.setString(1, e.company_name);
            ResultSet result = payrollUpdateStatement.executeQuery();
            while (result.next()) {
                e.company_id = result.getInt(1);
            }
            //Pushing employee details
            payrollUpdateStatement = c.prepareStatement("insert into employee(company_id,name,phone,address,gender) values (?,?,?,?,?)");
            payrollUpdateStatement.setInt(1, e.company_id);
            payrollUpdateStatement.setString(2, e.name);
            payrollUpdateStatement.setString(3, e.phone);
            payrollUpdateStatement.setString(4, e.address);
            payrollUpdateStatement.setString(5, Character.toString(e.gender));
            payrollUpdateStatement.executeUpdate();
            //Pushing department details
            payrollUpdateStatement = c.prepareStatement("insert into department(department_name) values (?)");
            payrollUpdateStatement.setString(1, e.department_name);
            payrollUpdateStatement.executeUpdate();
            //Getting the employee_id which is autoset in sql and setting it into the object
            payrollUpdateStatement = c.prepareStatement("select emp_id from employee where name=?");
            payrollUpdateStatement.setString(1, e.name);
            ResultSet result1 = payrollUpdateStatement.executeQuery();
            while (result1.next()) {
                e.emp_id = result1.getInt(1);
            }
            //Getting the employee_id which is autoset in sql and setting it into the object
            payrollUpdateStatement = c.prepareStatement("select department_id from department where department_name=?");
            payrollUpdateStatement.setString(1, e.department_name);
            ResultSet rs = payrollUpdateStatement.executeQuery();
            while (rs.next()) {
                e.department_id = rs.getInt(1);
                break;
            }
            //Linking the employee_id to department_id
            payrollUpdateStatement = c.prepareStatement("insert into employee_department(emp_id,department_id) values (?,?)");
            payrollUpdateStatement.setInt(1, e.emp_id);
            payrollUpdateStatement.setInt(2, e.department_id);
            payrollUpdateStatement.executeUpdate();
            //Pushing payroll details
            payrollUpdateStatement = c.prepareStatement("insert into payroll(emp_id,basic_pay,deductions,taxable_pay,tax,net_pay) values (?,?,?,?,?,?)");
            payrollUpdateStatement.setInt(1, e.emp_id);
            payrollUpdateStatement.setInt(2, e.basic_pay);
            payrollUpdateStatement.setInt(3, e.deductions);
            payrollUpdateStatement.setInt(4, e.taxable_pay);
            payrollUpdateStatement.setInt(5, e.tax);
            payrollUpdateStatement.setInt(6, e.net_pay);
            payrollUpdateStatement.executeUpdate();
            c.commit();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            try {
                c.rollback();
            } catch (SQLException sqlException) {
                sqlException.printStackTrace();
            }
        }
    }

    public void cascadingDelete(String name) {
        try {
            Connection c = con.getConnection();
            int id = 0;
            payrollUpdateStatement = c.prepareStatement("select emp_id from employee where name=?");
            payrollUpdateStatement.setString(1, name);
            ResultSet result1 = payrollUpdateStatement.executeQuery();
            while (result1.next()) {
                id = result1.getInt(1);
                break;
            }
            //Clear Payroll table
            payrollUpdateStatement = c.prepareStatement("delete from payroll where emp_id=?");
            payrollUpdateStatement.setInt(1, id);
            payrollUpdateStatement.executeUpdate();
            //Clear Employee_Department table
            payrollUpdateStatement = c.prepareStatement("delete from employee_department where emp_id=?");
            payrollUpdateStatement.setInt(1, id);
            payrollUpdateStatement.executeUpdate();
            //Clear Employee_table
            payrollUpdateStatement = c.prepareStatement("delete from employee where emp_id=?");
            payrollUpdateStatement.setInt(1, id);
            payrollUpdateStatement.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public int getSum() {
        int count = 0;
        Connection c = con.getConnection();
        try {
            payrollUpdateStatement = c.prepareStatement("select count(name) from employee where gender='F'");
            ResultSet result = payrollUpdateStatement.executeQuery();
            while (result.next()) {
                count++;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return count;
    }

    public void update(String name, int salary) {
        try {
            Connection c = con.getConnection();
            payrollUpdateStatement = c.prepareStatement("update payroll set basic_pay= ? where emp_id=(select emp_id from employee where name = ?);");
            payrollUpdateStatement.setInt(1, salary);
            payrollUpdateStatement.setString(2, name);
            payrollUpdateStatement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}