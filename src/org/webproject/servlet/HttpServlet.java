package org.webproject.servlet;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webproject.servlet.DBUtility;

/**
 * Servlet implementation class HttpServlet
 */
@WebServlet("/HttpServlet")
public class HttpServlet extends javax.servlet.http.HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     //* @see javax.servlet.http.HttpServlet#javax.servlet.http.HttpServlet()
     */
    public HttpServlet() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */

    protected void doPost(HttpServletRequest request, HttpServletResponse
            response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String tab_id = request.getParameter("tab_id");

        // create a report
        if (tab_id.equals("0")) {
            System.out.println("A report is submitted!");
            try {
                createReport(request, response);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // query reports
        else if (tab_id.equals("1")) {
            try {
                queryReport(request, response);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void createReport(HttpServletRequest request, HttpServletResponse
            response) throws SQLException, IOException {
        DBUtility dbutil = new DBUtility();
        String sql;


        // 1. create user
        int user_id = 0;
        String fN = request.getParameter("fN");
        String lN = request.getParameter("lN");
        String tel = request.getParameter("tel");
        String email = request.getParameter("email");
        if (fN != null) {fN = "'" + fN + "'";}
        if (lN != null) {lN = "'" + lN + "'";}
        if (tel != null) {tel = "'" + tel + "'";}
        if (email != null) {email = "'" + email + "'";}

        sql = "insert into person (first_name, last_name, " +
                "telephone, email) values (" + fN +
                "," + lN + "," + "," + tel +
                "," + email;

        dbutil.modifyDB(sql);

        // record user_id
        ResultSet res_2 = dbutil.queryDB("select last_value from person_id_seq");
        res_2.next();
        user_id = res_2.getInt(1);

        System.out.println("Success! User created.");

        // 2. create report
        int report_id = 0;
        String report_type = request.getParameter("report_type");
        String lon = request.getParameter("longitude");
        String lat = request.getParameter("latitude");
        String add_msg = request.getParameter("obstruction_or_restriction");
        String additionalinfo = request.getParameter("additionalinformation");
        //String obstruction_type = request.getParameter("obstruction_type");
        //String ada_restriction = request.getParameter("ada_restriction");

        if (report_type != null) {report_type = "'" + report_type + "'";}
        if (additionalinfo != null) {additionalinfo = "'" + additionalinfo + "'";}
        if (add_msg != null) {add_msg = "'" + add_msg + "'";}

        sql = "insert into report (reportor_id, report_type, geom," +
                " addtionalinformation) values (" + user_id + "," + report_type + ","
                + ", ST_GeomFromText('POINT(" + lon + " " + lat + ")', 4326)" + "," +
                additionalinfo + ")";
        dbutil.modifyDB(sql);

        // record report_id
        ResultSet res_3 = dbutil.queryDB("select last_value from report_id_seq");
        res_3.next();
        report_id = res_3.getInt(1);

        System.out.println("Success! Report created.");

        // 3. create specific report
        if (report_type.equals("'bike'")) {
            sql = "insert into bike_report (report_id, obstruction_type) values ('"
                    + report_id + "'," + add_msg + ")";
            System.out.println("Success! Bike report created.");
        } else if (report_type.equals("'pedestrian'")) {
            sql = "insert into pedestrian_report (report_id, obstruction_type) values ('"
                    + report_id + "'," + add_msg + ")";
            System.out.println("Success! Pedestrian report created.");
        } else if (report_type.equals("'ADA'")) {
            //************************Change to allow both obstruction_type and ada_restriction*********
            sql = "insert into ada_report (report_id, ada_restriction) values ('"
                    + report_id + "'," + add_msg + ")";
            System.out.println("Success! ADA report created.");
        } else {
            return;
        }
        dbutil.modifyDB(sql);

        // response that the report submission is successful
        JSONObject data = new JSONObject();
        try {
            data.put("status", "success");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        response.getWriter().write(data.toString());

    }

    private void queryReport(HttpServletRequest request, HttpServletResponse
            response) throws JSONException, SQLException, IOException {
        JSONArray list = new JSONArray();

        String report_type = request.getParameter("report_type");
        String obstruction_or_ada = request.getParameter("obstruction_or_ada");

        // bike report
        if (report_type == null || report_type.equalsIgnoreCase("bike")) {
            String sql = "select report.id, report_type, obstruction_type, " +
                    "first_name, last_name, time_stamp, ST_X(geom) as " +
                    "longitude, ST_Y(geom) as latitude, additionalinformation from report, person, " +
                    "bike_report where reporter_id = person.id and report.id = " +
                    "report_id";
            queryReportHelper(sql,list,"bike", obstruction_or_ada);
        }

        // pedestrian report
        if (report_type == null || report_type.equalsIgnoreCase("pedestrian")) {
            String sql = "select report.id, report_type, obstruction_type, " +
                    "first_name, last_name, time_stamp, ST_X(geom) as " +
                    "longitude, ST_Y(geom) as latitude, additionalinformation from report, person, " +
                    "pedestrian_report where reporter_id = person.id and report.id = " +
                    "report_id";
            queryReportHelper(sql,list,"pedestrian", obstruction_or_ada);
        }

        // ADA report
        if (report_type == null || report_type.equalsIgnoreCase("ADA")) {
            String sql = "select report.id, report_type, ada_restriction" +
                    "first_name, last_name, time_stamp, ST_X(geom) as " +
                    "longitude, ST_Y(geom) as latitude, additionalinformation from report, person, " +
                    "ada_report where reporter_id = person.id and report.id = " +
                    "report_id";
            queryReportHelper(sql,list,"ADA", obstruction_or_ada);
        }

        response.getWriter().write(list.toString());
    }

    private void queryReportHelper(String sql, JSONArray list, String report_type,
                                   String obstruction_or_ada) throws SQLException {
        DBUtility dbutil = new DBUtility();

        if (obstruction_or_ada != null) {
            if (report_type.equalsIgnoreCase("ada")) {
                sql += " and ada_restriction = '" + obstruction_or_ada + "'";
            } else {
                sql += " and obstruction_type = '" + obstruction_or_ada + "'";
            }
        }
        ResultSet res = dbutil.queryDB(sql);
        while (res.next()) {
            // add to response
            HashMap<String, String> m = new HashMap<String,String>();
            m.put("report_id", res.getString("id"));
            m.put("report_type", res.getString("report_type"));
            if (report_type.equalsIgnoreCase("bike") ||
                    report_type.equalsIgnoreCase("pedestrian")) {
                m.put("obstruction_type", res.getString("obstruction_type"));
            }
            else if (report_type.equalsIgnoreCase("ada")) {
                m.put("ada_restriction", res.getString("ada_restriction"));
            }
            m.put("first_name", res.getString("first_name"));
            m.put("last_name", res.getString("last_name"));
            m.put("time_stamp", res.getString("time_stamp"));
            m.put("longitude", res.getString("longitude"));
            m.put("latitude", res.getString("latitude"));
            m.put("additionalinformation", res.getString("additionalinformation"));
            list.put(m);
        }
    }

    public void main() throws JSONException {
    }
}