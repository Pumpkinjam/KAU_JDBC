import java.sql.SQLException;
import java.util.*;
import java.awt.*;  import java.awt.event.*;
import javax.swing.*;

public class GUI extends JFrame implements ActionListener {
    // TODO: Filtering JComboBox
    SQLConnector db;
    private JCheckBox[] cbList;
    private JButton btn_search = new JButton("검색");
    /*
    private JCheckBox cb_name = new JCheckBox("Name", true);
    private JCheckBox cb_ssn = new JCheckBox("Ssn", true);
    private JCheckBox cb_bdate = new JCheckBox("Bdate", true);
    private JCheckBox cb_address = new JCheckBox("Address", true);
    private JCheckBox cb_sex = new JCheckBox("Sex", true);
    private JCheckBox cb_salary = new JCheckBox("Salary", true);
    private JCheckBox cb_supervisor = new JCheckBox("Supervisor", true);
    private JCheckBox cb_department = new JCheckBox("Department", true);
    */
    Container c = this;

    GUI(String[] fields) {

        // CheckBoxes Initializing
        JCheckBox[] cbList = new JCheckBox[fields.length];
        for (int i = 0, z = fields.length; i < z; i++) {
            cbList[i] = new JCheckBox(fields[i], true);
        }

        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        checkboxPanel.add(new JLabel("검색 항목"));
        for (JCheckBox i : cbList) {
            checkboxPanel.add(i);
        }
        checkboxPanel.add(btn_search);

        // General Initializing
        c.add(checkboxPanel);
        setSize(1280, 720);
        setLocationRelativeTo(null);
        setTitle("KAU_JDBC");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.db = new SQLConnector("company", "root", "root");
        }
        catch (SQLException sqle) {
            System.err.println("SQL Connection failure.");
            sqle.printStackTrace();
        }
        catch(ClassNotFoundException cne) {
            System.err.println("Driver load failure.");
            cne.printStackTrace();
        }

    }
}
