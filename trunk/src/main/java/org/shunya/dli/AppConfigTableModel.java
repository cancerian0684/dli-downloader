package org.shunya.dli;

import javax.swing.table.AbstractTableModel;
import java.beans.Statement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class AppConfigTableModel extends AbstractTableModel {
    public static final int[] size = {120, 80, 300};
    public final List<String> mutableFields = asList("String", "int", "boolean");
    private static final long serialVersionUID = 1L;
    private final AppContext appContext;
    private final Map<String, Method> methods = new HashMap<>(100);

    private ArrayList<Field> data = new ArrayList<>();

    private String[] columnNames = new String[]
            {"<html><b>Property", "<html><b>Value", "<html><b>Description"};
    private Class[] columnClasses = new Class[]
            {String.class, String.class, String.class};


    public AppConfigTableModel(AppContext appContext) {
        this.appContext = appContext;
        Class aClass = AppContext.class;
        Field[] fields = aClass.getDeclaredFields();
        for (Field field : fields) {
            final Settings annotation = field.getAnnotation(Settings.class);
            if (annotation != null && !annotation.ignore()) {
                data.add(field);
                field.setAccessible(true);
            }
//            System.out.println(field.getType().getSimpleName());
        }

        final Method[] declaredMethods = aClass.getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            final String name = declaredMethod.getName();
            methods.put(name.toLowerCase(), declaredMethod);
        }
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return data.size();
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public Object getValueAt(int row, int col) {
        Field field = data.get(row);
        switch (col) {
            case 0:
                return field.getName();
            case 1:
                try {
                    return field.get(appContext);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            case 2:
                final Settings annotation = field.getAnnotation(Settings.class);
                return "<html>" + annotation.description();
        }
        return "";
    }

    public Class<?> getColumnClass(int col) {
        return columnClasses[col];
    }

    public void setValueAt(Object obj, int row, int col) {
        Field field = data.get(row);
        if (field != null) {
            try {
                switch (col) {
                    case 1:
                        if (field.getType().getSimpleName().equalsIgnoreCase("int")) {
                            new Statement(appContext, methods.get("set" + field.getName().toLowerCase()).getName(), new Object[]{Integer.parseInt((String) obj)}).execute();
                        } else if (field.getType().getSimpleName().equalsIgnoreCase("boolean")) {
                            field.set(appContext, Boolean.parseBoolean((String) obj));
                        } else if (field.getType().getSimpleName().equalsIgnoreCase("string")) {
                            field.set(appContext, obj);
                        }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.fireTableDataChanged();
    }


    public boolean isCellEditable(int row, int col) {
        if (col == 1) {
            if (mutableFields.contains(data.get(row).getType().getSimpleName()) && data.get(row).getAnnotation(Settings.class).editable()) {
                return true;
            }
        }
        return false;
    }
}