import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.awt.*;  import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

class GUI extends JFrame implements ActionListener {
    SQLConnector db;

    JPanel motherPanel = new JPanel();

    JPanel filterPanel;

    JPanel conditionFilterPanel;
    private final JComboBox category;
    private final JTextField condition_input;

    JPanel fieldFilterPanel;
    private final JCheckBox[] cbList;
    private final JButton btn_search = new JButton("검색");

    JPanel underPanel;

    private static final String[] fields =  {"Fname", "Minit", "Lname", "Ssn", "Bdate", "Address", "Sex", "Salary", "Super_ssn", "Dno"};
    private static final String[] displayedFields = {"Name", "Ssn", "Bdate", "Address", "Sex", "Salary", "Supervisor", "Department"};
    JPanel resultPanel;
    DefaultTableModel model, hiddenModel = null;
    JTable resultTable;
    JScrollPane sPane;

    JPanel editButtonPanel;
    JPanel insertPanel, updatePanel, deletePanel;
    HintTextField[] insertForm, updateForm;
    JButton insertConfirmButton, updateConfirmButton, deleteConfirmButton;

    Vector<String> lastSearchField;
    String lastSearchStatement;
    String lastSelect, lastFrom, lastWhere;

    // Construct with field names (in Checkboxes)
    GUI() {

        /* db Connecting
         */
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException cne) {
            System.err.println("Driver load failure.");
            cne.printStackTrace();
            System.exit(-1);
        }

        // input password from user, and then try to connect.
        try { this.db = new SQLConnector("company", "root", JOptionPane.showInputDialog("Input password")); }
        catch (SQLException sqle) {
            System.err.println("SQL Connection failure.");
            sqle.printStackTrace();
            System.exit(-1);
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
        cbList = new JCheckBox[displayedFields.length];
        for (int i = 0, z = displayedFields.length; i < z; i++) {
            cbList[i] = new JCheckBox(displayedFields[i], true);
        }

        fieldFilterPanel = new JPanel();
        fieldFilterPanel.add(new JLabel("검색 항목"));
        for (JCheckBox i : cbList) {
            fieldFilterPanel.add(i);
        }
        fieldFilterPanel.add(btn_search);
        btn_search.addActionListener(this);

        filterPanel.add(fieldFilterPanel);

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

        insertForm = new HintTextField[fields.length];
        for (int i = 0, z = fields.length; i < z; i++) {
            insertForm[i] = new HintTextField(fields[i]);
            insertForm[i].setPreferredSize(new Dimension(60, 20));
            insertPanel.add(insertForm[i]);
        }
        insertConfirmButton = new JButton("Insert");
        insertConfirmButton.addActionListener(this);
        insertPanel.add(insertConfirmButton);

        updateForm = new HintTextField[2];
        updateForm[0] = new HintTextField("Field name");
        updateForm[1] = new HintTextField("Change to...");
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

        underPanel.add("North", resultPanel);
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


    // not checking the category/condition, just execute query {st}, and then update the table
    private void refreshTable() {
        String st = lastSearchStatement;
        Vector<String> fieldVector = lastSearchField;

        // hidden model exists for getting ssn anytime.
        hiddenModel = new DefaultTableModel(fieldVector, 0);
        try {
            db.setStatement("SELECT a.Ssn FROM EMPLOYEE a LEFT OUTER JOIN EMPLOYEE b ON a.Super_ssn=b.Ssn, DEPARTMENT WHERE " + lastWhere);
            ResultSet r = db.getResultSet();

            while (r.next()) {
                Vector<String> tuple = new Vector<>();
                tuple.add(r.getString("Ssn"));
                hiddenModel.addRow(tuple);
            }
        } catch (SQLException sqle) {
            alert("Error occured during setting hidden model.");
            sqle.printStackTrace();
            return;
        }

        // now, let the result table be shown
        model = new DefaultTableModel(fieldVector, 0);

        try {
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
            alert("Error occured during setting table.");
            sqle.printStackTrace();
            return;
        }

        resultTable = new JTable(model) /*{
            @Override
            public void columnSelectionChanged(ListSelectionEvent e) {
                GUI.selectedRow = getSelectedRow();
            }
        }*/;

        //resultTable.setMaximumSize(new Dimension(1000, 200));
        sPane = new JScrollPane(resultTable);
        sPane.setPreferredSize(new Dimension(1000, 200));

        resultPanel.removeAll();
        resultPanel.add(sPane);
        this.revalidate();
    }

    // called when btn_search clicked.
    // get tuples and show it to the table.
    private void searchServiceRoutine() {
        Vector<String> fieldVector = new Vector<>();    // for head row (names of column)
        lastSelect = ""; lastFrom = ""; lastWhere = "";

        boolean firstFieldExists = false;                    // we must handle attaching comma (',')

        // Query Statement Building
        for (int i = 0, z = displayedFields.length; i < z; i++) {
            // check if checkboxes selected
            if (cbList[i].isSelected()) {
                fieldVector.add(displayedFields[i]);     // add to head row

                if (firstFieldExists)   lastSelect += ", ";
                else                    firstFieldExists = true;

                String selectedString = displayedFields[i];

                switch (selectedString) {
                    case "Name" -> lastSelect += "concat(a.fname, ' ', a.minit, ' ', a.lname) as Name";
                    case "Supervisor" -> lastSelect += "concat(b.fname, ' ', b.minit, ' ', b.lname) as Supervisor";
                    case "Department" -> lastSelect += "dname as Department";
                    default -> lastSelect += "a." + selectedString;
                }

            }
        }

        if (!firstFieldExists) {
            System.out.println("체크박스를 하나 이상 선택하십시오.");
            return;
        }
        lastFrom += "EMPLOYEE a LEFT OUTER JOIN EMPLOYEE b ON a.Super_ssn=b.Ssn, DEPARTMENT";
        //st += " WHERE a.super_ssn=b.ssn AND a.dno=dnumber";
        lastWhere += "a.dno=dnumber";   // natural join으로는 super_ssn이 null인 경우를 가져올 수 없음

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
        switch (selectedCategory) {
            case "부서" -> lastWhere += " AND Dname='" + selectedCondition + "'";

            // 성별으로 검색 (M or F)
            case "성별" -> lastWhere += " AND a.Sex='" + selectedCondition + "'";

            // 입력한 값보다 높은 연봉을 받는 직원 검색
            case "연봉" -> lastWhere += " AND a.Salary>" + selectedCondition;

            // 생일이 n월인 직원 검색 (1월 ~ 12월)
            // 정수만 입력 가능, "월" 붙여도 처리 가능
            // TODO: additional: "년", "일" 붙여도 처리하는 기능 (이 경우 정수만 입력할 수는 없음)
            case "생일" -> {
                int l = selectedCondition.length();
                // "월" 붙은 경우 "월" 제거한 뒤 비교
                if (selectedCondition.charAt(l - 1) == '월') {
                    selectedCondition = selectedCondition.substring(0, l - 1);
                }
                lastWhere += " AND MONTH(a.Bdate)=" + selectedCondition;
            }
            // Ssn(에 해당하는 직원)을 상사로 갖는 직원 검색
            case "부하직원" -> lastWhere += " AND b.ssn=" + quote(selectedCondition);
        }

        String st = "SELECT " + lastSelect +
                " FROM " + lastFrom +
                " WHERE " + lastWhere;

        System.out.println("Query Statement : " + st);
        lastSearchStatement = st;
        lastSearchField = fieldVector;

        refreshTable();
    }

    //  0      1      2       3    4      5        6    7       8           9         10       11
    // {Fname, Minit, Lname}, Ssn, Bdate, Address, Sex, Salary, Super_ssn, "Dname!!", created, modified
    private void insertServiceRoutine() {
        // boolean isFirst = true;

        String st = "INSERT INTO EMPLOYEE VALUES (?,?,?,?,?,?,?,?,?,?,?,?);";
        Vector<String> arguments = new Vector<>();

        for (HintTextField tf : insertForm) {
            String s = tf.contentLen == 0 ? "null" : tf.getText();

            if (tf.getHint().equals("Dno")) {
                try {
                    // null processing for dname
                    if (s.equals("null")) {
                        arguments.add("5"); continue;
                    }

                    db.setStatement("SELECT Dnumber FROM DEPARTMENT WHERE Dname=" + quote(s) + ";");
                    ResultSet r = db.getResultSet();

                    r.next();
                    arguments.add(r.getString("Dnumber"));
                }
                catch (SQLException sqle) {
                    alert("An error occurred during getting Dname");
                    sqle.printStackTrace();
                    return;
                }
            }
            else {
                arguments.add(s);
            }

        }

        DateTime nowTime = DateTime.now();

        arguments.add(nowTime.toString());
        arguments.add(nowTime.toString());

        System.out.println("Query Statement : " + st);

        for (String s : arguments) {
            System.out.print(s + " ");
        }
        System.out.println();

        try {
            db.setStatement(st);
            db.setStrings(arguments);
            db.update();
        }
        catch (SQLException sqle) {
            alert("Error occurred during inserting tuple.");
            sqle.printStackTrace();
            return;
        }

        try {
            refreshTable();
        }
        catch (Exception e) {
            alert("Error occurred during refreshing table: after inserting data.");
            e.printStackTrace();
            return;
        }
        System.out.println("Insert Succeed.");
    }

    private void updateServiceRoutine() {
        int rowIdx;

        try {
            rowIdx = resultTable.getSelectedRow();
            if (rowIdx == -1) {
                alert("행을 선택해주세요.");
                return;
            }
        } catch (NullPointerException npe) {
            alert("테이블이 생성되지 않았습니다.");
            npe.printStackTrace();
            return;
        }

        String selected = updateForm[0].getText();
        Vector<String> args = new Vector<>();
        String st;

        if (selected.equalsIgnoreCase("name")) {
            FullName name = new FullName(updateForm[1].getText());
            st = "UPDATE EMPLOYEE SET Fname=?, Minit=?, Lname=?, modified=? WHERE Ssn=" +
                    quote((String) hiddenModel.getValueAt(rowIdx, 0)) + ";";
            args.add(name.Fname);
            args.add(name.minit == 0 ? "" : ""+name.minit);
            args.add(name.Lname);
        }
        else {
            st = "UPDATE EMPLOYEE SET " + selected + "=?, modified=? WHERE Ssn=" +
                    quote((String) hiddenModel.getValueAt(rowIdx, 0)) + ";";
            args.add(updateForm[1].getText());
        }

        args.add(DateTime.now().toString());
        System.out.println("Query Statement : " + st);

        try {
            db.setStatement(st);
            db.setStrings(args);
            db.update();
        }
        catch (SQLException sqle) {
            alert("Error occurred during updating data.");
            sqle.printStackTrace();
            return;
        }

        try {
            refreshTable();
        } catch (Exception e) {
            alert("Error occurred during refreshing table: after updating data.");
            e.printStackTrace();
            return;
        }

        System.out.println("updateServiceRoutine succeed.");
    }

    private void deleteServiceRoutine() {
        int rowIdx;

        try {
            rowIdx = resultTable.getSelectedRow();
            if (rowIdx == -1) {
                alert("행을 선택해주세요.");
                return;
            }
        } catch (NullPointerException npe) {
            alert("테이블이 생성되지 않았습니다.");
            npe.printStackTrace();
            return;
        }

        String st = "DELETE FROM EMPLOYEE WHERE ";
        st += "Ssn=" + quote((String)hiddenModel.getValueAt(rowIdx, 0)) + ";";

        System.out.println("Query Statement : " + st);

        try {
            db.setStatement(st);
            db.update();
        }
        catch (SQLException sqle) {
            alert("Error occurred during updating data.");
            sqle.printStackTrace();
            return;
        }

        model.removeRow(rowIdx);


        try {
            refreshTable();
        } catch (Exception e) {
            alert("Error occurred during refreshing table: after updating data.");
            e.printStackTrace();
            return;
        }

        refreshTable();
        System.out.println("deleteServiceRoutine succeed.");

    }


    @Override
    public void actionPerformed(ActionEvent e) {

        Object trg = e.getSource();

        if (trg == btn_search) searchServiceRoutine();
        else if (trg == insertConfirmButton) insertServiceRoutine();
        else if (trg == updateConfirmButton) updateServiceRoutine();
        else if (trg == deleteConfirmButton) deleteServiceRoutine();
        else {
            // TODO: make buttons, and then add actions here
            System.out.println("That button does not have actions yet.");
        }

    }

    public static void alert(String msg) {
        System.out.println(msg);
        JOptionPane.showMessageDialog(null, msg);
    }

    private static String quote(String s) { return "'" + s + "'"; }
}

/* get helped from...
 * https://hwangcoding.tistory.com/15
 */
class HintTextField extends JTextField {
    private final String hint;
    int contentLen;
    String lastText;

    HintTextField(String hint) {
        setText(hint);

        this.hint = hint;
        // HintTextField displays hint if (contentLen == 0)
        this.contentLen = 0;
        this.lastText = "";

        setForeground(Color.GRAY);

        this.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (contentLen == 0) {
                    setText("");
                    setForeground(Color.BLACK);
                }
                lastText = getText();
            }

            @Override
            public void focusLost(FocusEvent e) {
                contentLen = getText("").length();

                if (contentLen == 0) {
                    setText(hint);
                    contentLen=0;
                    setForeground(Color.GRAY);
                }
                else {
                    setText(getText());
                    setForeground(Color.BLACK);
                }
            }
        });
    }

    @Override
    public void setText(String t) {
        super.setText(t);
        this.contentLen = t.length();
    }
    @Override
    public String getText() {
        if (this.contentLen == 0) return "";
        return super.getText();
    }

    public String getHint() {return this.hint;}

    // if you don't want to check contentLen
    public String getText(Object o) {
        return super.getText();
    }

}

class FullName {
    String Fname, Lname;
    char minit;

    FullName(String s) {
        minit = 0;

        StringTokenizer nameTokenizer = new StringTokenizer(s);
        Fname = nameTokenizer.nextToken();
        Lname = nameTokenizer.nextToken();
        if (nameTokenizer.hasMoreTokens()) {
            minit = Lname.charAt(0);
            Lname = nameTokenizer.nextToken();
        }
    }

    public String[] getStringArray() {
        String[] res;
        if (minit == 0) {
            res = new String[2];
            res[0] = Fname; res[1] = Lname;
        }
        else {
            res = new String[3];
            res[0] = Fname; res[1] = minit + ""; res[2] = Lname;
        }
        return res;
    }

    @Override
    public String toString() {
        return Fname + " " + minit + " " + Lname;
    }
}


// I wanted to extend java.time.LocalDateTime
class DateTime {
    LocalDateTime dt;

    DateTime(LocalDateTime dt) {
        this.dt = dt;
    }

    public static DateTime now() {
        return new DateTime(LocalDateTime.now());
    }

    @Override
    // "yyyy-mm-dd HH:MM:SS"
    public String toString() {
        return dt.getYear() + "-" + dt.getMonthValue() + "-" + dt.getDayOfMonth() + " " +
                dt.getHour() + ":" + dt.getMinute() + ":" + dt.getSecond();
    }
}