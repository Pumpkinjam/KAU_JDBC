import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.awt.*;  import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;

class GUI extends JFrame implements ActionListener {
    SQLConnector db;

    JPanel motherPanel = new JPanel();

    JPanel filterPanel;

    JPanel conditionFilterPanel;
    private JComboBox category;
    private JTextField condition_input;

    JPanel fieldFilterPanel;
    private JCheckBox[] cbList;
    private JButton btn_search = new JButton("검색");

    JPanel underPanel;

    String[] fields;
    JPanel resultPanel;
    DefaultTableModel model;
    JTable resultTable;
    JScrollPane sPane;

    JPanel editButtonPanel;
    JPanel insertPanel, updatePanel, deletePanel;
    JTextField[] insertForm, updateForm;
    JButton insertConfirmButton, updateConfirmButton, deleteConfirmButton;

    boolean resetNeeded = false;

    // Construct with field names (in Checkboxes)
    GUI(String[] fields) {
        this.fields = fields;

        /* db Connecting
         */
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException cne) {
            System.err.println("Driver load failure.");
            cne.printStackTrace();
            //System.exit(0); TODO: uncomment this line.
        }

        // input password from user, and then try to connect.
        try { this.db = new SQLConnector("company", "root", JOptionPane.showInputDialog("Input password")); }
        catch (SQLException sqle) {
            System.err.println("SQL Connection failure.");
            sqle.printStackTrace();
            //System.exit(0); TODO: uncomment this line too.
        }

        System.out.println("Database Connection Succeed.");

        motherPanel.setLayout(new BoxLayout(motherPanel, BoxLayout.Y_AXIS));
        /* Panel for Filters Initializing
         */
        filterPanel = new JPanel();
        filterPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        /* ConditionFilter GUI (+ComboBox) Initializing
         */
        conditionFilterPanel = new JPanel();
        String[] arrayOfCategories = {"전체", "부서", "성별", "연봉", "생일", "부하직원"};
        category = new JComboBox(arrayOfCategories);
        condition_input = new JTextField(15);

        conditionFilterPanel.add(category);
        conditionFilterPanel.add(condition_input);

        filterPanel.add(conditionFilterPanel);

        /* FieldFilter GUI (+CheckBox) Initializing
         */
        cbList = new JCheckBox[fields.length];
        for (int i = 0, z = fields.length; i < z; i++) {
            cbList[i] = new JCheckBox(fields[i], true);
        }

        fieldFilterPanel = new JPanel();
        fieldFilterPanel.add(new JLabel("검색 항목"));
        for (JCheckBox i : cbList) {
            fieldFilterPanel.add(i);
        }
        fieldFilterPanel.add(btn_search);
        btn_search.addActionListener(this);

        filterPanel.add(fieldFilterPanel);

        /* Search Result Table (+ScrollPane) late Initializing
        model = new DefaultTableModel();
        resultTable = new JTable(model);
        sPane = new JScrollPane(resultTable);
        sPane.setSize(1000, 500);

        resultPanel = new JPanel();
        resultPanel.setSize(1100, 550);
        */
        resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setPreferredSize(new Dimension(1000, 200));

        editButtonPanel = new JPanel();
        editButtonPanel.setLayout(new BoxLayout(editButtonPanel, BoxLayout.X_AXIS));
        insertPanel = new JPanel();
        updatePanel = new JPanel();
        deletePanel = new JPanel();
        editButtonPanel.add(insertPanel);
        editButtonPanel.add(updatePanel);
        editButtonPanel.add(deletePanel);

        insertForm = new JTextField[fields.length];
        for (int i = 0, z = fields.length; i < z; i++) {
            insertForm[i] = new HintTextField(fields[i]);
            insertPanel.add(insertForm[i]);
        }
        insertConfirmButton = new JButton("Insert");
        insertConfirmButton.addActionListener(this);
        insertPanel.add(insertConfirmButton);

        updateForm = new JTextField[2];
        updateForm[0] = new HintTextField("Field name to be updated");
        updateForm[1] = new HintTextField("Update to...");
        updatePanel.add(updateForm[0]);
        updatePanel.add(updateForm[1]);
        updateConfirmButton = new JButton("Update");
        updateConfirmButton.addActionListener(this);
        updatePanel.add(updateConfirmButton);

        deleteConfirmButton = new JButton("Delete");
        deleteConfirmButton.addActionListener(this);
        deletePanel.add(deleteConfirmButton);

        underPanel = new JPanel();
        underPanel.setLayout(new BorderLayout());

        underPanel.add(resultPanel);
        underPanel.add("South", editButtonPanel);

        /* General Initializing
         */
        motherPanel.add(filterPanel);
        motherPanel.add(underPanel);
        this.add(motherPanel);
        setSize(1280, 720);
        setLocationRelativeTo(null);
        setTitle("KAU_JDBC");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    // called when btn_search clicked.
    // get tuples and show it to the table.
    private void searchServiceRoutine() {
        Vector<String> fieldVector = new Vector<>();    // for head row (names of column)

        String st = "SELECT ";                           // statement string ready
        boolean firstFieldExists = false;                    // we must handle attaching comma (',')

        // Query Statement Building
        for (int i = 0, z = fields.length; i < z; i++) {
            // check if checkboxes selected
            if (cbList[i].isSelected()) {
                fieldVector.add(fields[i]);     // add to head row

                if (firstFieldExists)   st += ", ";
                else                    firstFieldExists = true;

                String selectedString = fields[i];

                if (selectedString.equals("Name")) {
                    st += "concat(a.fname, ' ', a.minit, ' ', a.lname) as Name";
                }
                else if (selectedString.equals("Supervisor")) {
                    st += "concat(b.fname, ' ', b.minit, ' ', b.lname) as Supervisor";
                }
                else if (selectedString.equals("Department")) {
                    st += "dname as Department";
                }
                else {
                    st += "a."+selectedString;
                }

            }
        }

        if (!firstFieldExists) {
            System.out.println("체크박스를 하나 이상 선택하십시오.");
            resetNeeded = false;
            return;
        }
        st += " FROM EMPLOYEE a LEFT OUTER JOIN EMPLOYEE B ON a.Super_ssn=b.Ssn, DEPARTMENT";
        //st += " WHERE a.super_ssn=b.ssn AND a.dno=dnumber";
        st += " WHERE a.dno=dnumber";   // natural join으로는 super_ssn이 null인 경우를 가져올 수 없음

        // ..and additional condition statements
        // "전체", "부서", "성별", "연봉", "생일", "부하직원"
        String selectedCategory = category.getSelectedItem().toString();
        String selectedCondition = condition_input.getText();

        // 범위를 설정했으면서 검색 조건을 설정하지 않았다면?
        // 범위를 설정하지 않은 경우로 검색
        if (!selectedCategory.equals("전체") && selectedCondition.equals("")) {
            System.out.println("조건을 입력하지 않아 전체를 검색합니다.");
            selectedCategory = "전체";
        }

        // 부서명으로 검색
        if (selectedCategory.equals("부서")) {
            st += " AND Dname=\"" + selectedCondition + "\"";
        }
        // 성별으로 검색 (M or F)
        else if (selectedCategory.equals("성별")) {
            st += " AND a.Sex=\"" + selectedCondition + "\"";
        }
        // 입력한 값보다 높은 연봉을 받는 직원 검색
        else if (selectedCategory.equals("연봉")) {
            st += " AND a.Salary>" + selectedCondition;
        }
        // 생일이 n월인 직원 검색 (1월 ~ 12월)
        // 정수만 입력 가능, "월" 붙여도 처리 가능
        // TODO: additional: "년", "일" 붙여도 처리하는 기능 (이 경우 정수만 입력할 수는 없음)
        else if (selectedCategory.equals("생일")) {
            int l = selectedCondition.length();
            // "월" 붙은 경우 "월" 제거한 뒤 비교
            if (selectedCondition.charAt(l-1) == '월') {
                selectedCondition = selectedCondition.substring(0, l-1);
            }

            st += " AND MONTH(a.Bdate)=" + selectedCondition;
        }
        // Ssn(에 해당하는 직원)을 상사로 갖는 직원 검색
        else if (selectedCategory.equals("부하직원")) {
            st += " AND a.ssn=b.super_ssn";
        }

        st += ";";

        System.out.println("Query Statement : " + st);

        // now, let the result table be shown
        model = new DefaultTableModel(fieldVector, 0);

        try {
            resetNeeded = true;
            db.setStatement(st);
            ResultSet r = db.getResultSet();

            while (r.next()) {
                Vector<String> tuple = new Vector<>();
                for (String i : fieldVector) {
                    tuple.add(r.getString(i));
                }
                model.addRow(tuple);
            }

        }
        catch (SQLException sqle) {
            System.out.println("Error occured during setting table.");
            sqle.printStackTrace();
        }

        resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setPreferredSize(new Dimension(1000, 200));

        resultTable = new JTable(model);
        //resultTable.setMaximumSize(new Dimension(1000, 200));
        sPane = new JScrollPane(resultTable);
        sPane.setPreferredSize(new Dimension(1000, 200));

        resultPanel.add(sPane);
        underPanel.add("North", resultPanel);
        this.revalidate();

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        Object trg = e.getSource();

        if (trg == btn_search) searchServiceRoutine();
        else if (trg == insertConfirmButton) {
            boolean isFirst = true;
            String st = "INSERT INTO EMPLOYEE VALUES (";
            for (JTextField tf : insertForm) {
                String s = tf.getText();

                if (isFirst) isFirst = false;
                else st += ", ";

                st += (s == null) ? "NULL" : s;
            }
            st += ");";

            try {
                db.setStatement(st);
                db.update();
            }
            catch (SQLException sqle) {
                System.out.println("Error occured during inserting tuple.");
                sqle.printStackTrace();
            }

            alert("Insert Succeed.");
        }
        else if (trg == updateConfirmButton) {

        }
        else if (trg == deleteConfirmButton) {

        }
        else {
            // TODO: make buttons, and then add actions here
            System.out.println("That button does not have actions yet.");
        }




    }

    public static void alert(String msg) {
        System.out.println(msg);
        JOptionPane.showMessageDialog(null, msg);
    }
}

/* get helped from...
 * https://hwangcoding.tistory.com/15
 */
class HintTextField extends JTextField {
    String hint;
    int contentLen;

    HintTextField(String hint) {
        this.hint = hint;
        // HintTextField displays hint if (contentLen == 0)
        this.contentLen = 0;

        setText(hint);
        setForeground(Color.GRAY);

        this.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (contentLen == 0) {
                    setText("");
                    setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                contentLen = getText().length();

                if (contentLen == 0) {
                    setText(hint);
                    setForeground(Color.GRAY);
                }
                else {
                    setText(getText());
                    setForeground(Color.BLACK);
                }
            }
        });
    }

}