/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * The author of this program can be reached at thomas@infolab.northwestern.edu
 **/
package soc.server.database;

import soc.util.SOCRobotParameters;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.Calendar;


/**
 * This class contains methods for connecting to a database
 * and for manipulating the data stored there.
 *
 * Based on jdbc code found at www.javaworld.com
 *
 * @author Robert S. Thomas
 */
/**
 * This code assumes that you're using mySQL as your database.
 * It uses a database created with the following commands:
 * CREATE DATABASE socdata;
 * USE socdata;
 * CREATE TABLE users (nickname VARCHAR(20), host VARCHAR(50), password VARCHAR(20), email VARCHAR(50), lastlogin DATE);
 * CREATE TABLE logins (nickname VARCHAR(20), host VARCHAR(50), lastlogin DATE);
 * CREATE TABLE games (gamename VARCHAR(20), player1 VARCHAR(20), player2 VARCHAR(20), player3 VARCHAR(20), player4 VARCHAR(20), score1 TINYINT, score2 TINYINT, score3 TINYINT, score4 TINYINT, starttime TIMESTAMP);
 * CREATE TABLE robotparams (robotname VARCHAR(20), maxgamelength INT, maxeta INT, etabonusfactor FLOAT, adversarialfactor FLOAT, leaderadversarialfactor FLOAT, devcardmultiplier FLOAT, threatmultiplier FLOAT, strategytype INT, starttime TIMESTAMP, endtime TIMESTAMP, gameswon INT, gameslost INT, tradeFlag BOOL);
 *
 */
public class SOCDBHelper
{
    private static Connection connection = null;
    private static PreparedStatement _preparedStatementForCreateAccountCommand = null;
    private static String sCreateAccountCommand = "INSERT INTO users VALUES (?,?,?,?,?);";
    private static PreparedStatement _preparedStatementForRecordLoginCommand = null;
    private static String sRecordLoginCommand = "INSERT INTO logins VALUES (?,?,?);";
    private static PreparedStatement _preparedStatementForUserPasswordQuery = null;
    private static String sUserPasswordQuery = "SELECT password FROM users WHERE ( users.nickname = ? );";
    private static PreparedStatement _preparedStatementForHostQuery = null;
    private static String sHostQuery = "SELECT nickname FROM users WHERE ( users.host = ? );";
    private static PreparedStatement _preparedStatementForLastloginUpdate = null;
    private static String sLastloginUpdate = "UPDATE users SET lastlogin = ?  WHERE nickname = ? ;";
    private static PreparedStatement _preparedStatementForSaveGameCommand = null;
    private static String sSaveGameCommand = "INSERT INTO games VALUES (?,?,?,?,?,?,?,?,?,?);";
    private static PreparedStatement _preparedStatementForRobotParamsQuery = null;
    private static String sRobotParamsQuery = "SELECT * FROM robotparams WHERE robotname = ?;";

    /**
     * Closes the current conection to the database, opens a new one,
     * and initializes the prepared statements.
     *
     * @param user  the user name for accessing the database
     * @param pswd  the password for the user
     */
    public static void reconnect(String user, String pswd) throws SQLException
    {
        try
        {
            cleanup();

            String url = "jdbc:mysql://localhost/socdata";

            connection = DriverManager.getConnection(url, user, pswd);

            // prepare PreparedStatements for queries
            _preparedStatementForCreateAccountCommand = connection.prepareStatement(sCreateAccountCommand);
            _preparedStatementForRecordLoginCommand = connection.prepareStatement(sRecordLoginCommand);
            _preparedStatementForUserPasswordQuery = connection.prepareStatement(sUserPasswordQuery);
            _preparedStatementForHostQuery = connection.prepareStatement(sHostQuery);
            _preparedStatementForLastloginUpdate = connection.prepareStatement(sLastloginUpdate);
            _preparedStatementForSaveGameCommand = connection.prepareStatement(sSaveGameCommand);
            _preparedStatementForRobotParamsQuery = connection.prepareStatement(sRobotParamsQuery);
        }
        catch (java.lang.Exception ex)
        {
            // Got some other type of exception.  Dump it.
            ex.printStackTrace();
        }
    }

    /**
     * This makes a connection to the database
     * and initializes the prepared statements.
     *
     * @param user  the user name for accessing the database
     * @param pswd  the password for the user
     */
    public static void initialize(String user, String pswd) throws SQLException
    {
        try
        {
            // Load the mysql driver
            Class.forName("org.gjt.mm.mysql.Driver").newInstance();

            String url = "jdbc:mysql://localhost/socdata";

            connection = DriverManager.getConnection(url, user, pswd);

            // prepare PreparedStatements for queries
            _preparedStatementForCreateAccountCommand = connection.prepareStatement(sCreateAccountCommand);
            _preparedStatementForRecordLoginCommand = connection.prepareStatement(sRecordLoginCommand);
            _preparedStatementForUserPasswordQuery = connection.prepareStatement(sUserPasswordQuery);
            _preparedStatementForHostQuery = connection.prepareStatement(sHostQuery);
            _preparedStatementForLastloginUpdate = connection.prepareStatement(sLastloginUpdate);
            _preparedStatementForSaveGameCommand = connection.prepareStatement(sSaveGameCommand);
            _preparedStatementForRobotParamsQuery = connection.prepareStatement(sRobotParamsQuery);
        }
        catch (ClassNotFoundException cnfE)
        {
            throw new SQLException(cnfE.toString());
        }
        catch (java.lang.Exception ex)
        {
            // Got some other type of exception.  Dump it.
            ex.printStackTrace();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param sUserName DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws SQLException DOCUMENT ME!
     */
    public static String getUserPassword(String sUserName) throws SQLException
    {
        String password = null;

        // ensure that the JDBC connection is still valid
        if (connection == null)
        {
            return password;
        }

        try
        {
            // fill in the data values to the Prepared statement
            _preparedStatementForUserPasswordQuery.setString(1, sUserName);

            // execute the Query
            ResultSet resultSet = _preparedStatementForUserPasswordQuery.executeQuery();

            if (!resultSet.next())
            {
                // the database has no results - therefore the user is not authenticated
                resultSet.close();

                return password;
            }

            // retrieve the resultset
            password = resultSet.getString(1);
            resultSet.close();
        }
        catch (SQLException sqlE)
        {
            sqlE.printStackTrace();
            throw sqlE;
        }

        return password;
    }

    /**
     * DOCUMENT ME!
     *
     * @param host DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws SQLException DOCUMENT ME!
     */
    public static String getUserFromHost(String host) throws SQLException
    {
        String nickname = null;

        // ensure that the JDBC connection is still valid
        if (connection == null)
        {
            return nickname;
        }

        try
        {
            // fill in the data values to the Prepared statement
            _preparedStatementForHostQuery.setString(1, host);

            // execute the Query
            ResultSet resultSet = _preparedStatementForHostQuery.executeQuery();

            if (!resultSet.next())
            {
                // the database has no results - therefore the user is not authenticated
                resultSet.close();

                return nickname;
            }

            // retrieve the resultset
            nickname = resultSet.getString(1);
            resultSet.close();
        }
        catch (SQLException sqlE)
        {
            sqlE.printStackTrace();
            throw sqlE;
        }

        return nickname;
    }

    /**
     * DOCUMENT ME!
     *
     * @param userName DOCUMENT ME!
     * @param host DOCUMENT ME!
     * @param password DOCUMENT ME!
     * @param email DOCUMENT ME!
     * @param time DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws SQLException DOCUMENT ME!
     */
    public static boolean createAccount(String userName, String host, String password, String email, long time) throws SQLException
    {
        // ensure that the JDBC connection is still valid
        if (connection == null)
        {
            return false;
        }

        try
        {
            java.sql.Date sqlDate = new java.sql.Date(time);
            Calendar cal = Calendar.getInstance();

            // fill in the data values to the Prepared statement
            _preparedStatementForCreateAccountCommand.setString(1, userName);
            _preparedStatementForCreateAccountCommand.setString(2, host);
            _preparedStatementForCreateAccountCommand.setString(3, password);
            _preparedStatementForCreateAccountCommand.setString(4, email);
            _preparedStatementForCreateAccountCommand.setDate(5, sqlDate, cal);

            // execute the Command
            _preparedStatementForCreateAccountCommand.executeQuery();
        }
        catch (SQLException sqlE)
        {
            sqlE.printStackTrace();
            throw sqlE;
        }

        return true;
    }

    /**
     * DOCUMENT ME!
     *
     * @param userName DOCUMENT ME!
     * @param host DOCUMENT ME!
     * @param time DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws SQLException DOCUMENT ME!
     */
    public static boolean recordLogin(String userName, String host, long time) throws SQLException
    {
        // ensure that the JDBC connection is still valid
        if (connection == null)
        {
            return false;
        }

        try
        {
            java.sql.Date sqlDate = new java.sql.Date(time);
            Calendar cal = Calendar.getInstance();

            // fill in the data values to the Prepared statement
            _preparedStatementForRecordLoginCommand.setString(1, userName);
            _preparedStatementForRecordLoginCommand.setString(2, host);
            _preparedStatementForRecordLoginCommand.setDate(3, sqlDate, cal);

            // execute the Command
            _preparedStatementForRecordLoginCommand.executeQuery();
        }
        catch (SQLException sqlE)
        {
            sqlE.printStackTrace();
            throw sqlE;
        }

        return true;
    }

    /**
     * DOCUMENT ME!
     *
     * @param userName DOCUMENT ME!
     * @param time DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws SQLException DOCUMENT ME!
     */
    public static boolean updateLastlogin(String userName, long time) throws SQLException
    {
        // ensure that the JDBC connection is still valid
        if (connection == null)
        {
            return true;
        }

        try
        {
            java.sql.Date sqlDate = new java.sql.Date(time);
            Calendar cal = Calendar.getInstance();

            // fill in the data values to the Prepared statement
            _preparedStatementForLastloginUpdate.setDate(1, sqlDate, cal);
            _preparedStatementForLastloginUpdate.setString(2, userName);

            // execute the Command
            _preparedStatementForLastloginUpdate.executeQuery();
        }
        catch (SQLException sqlE)
        {
            sqlE.printStackTrace();
            throw sqlE;
        }

        return true;
    }

    /**
     * DOCUMENT ME!
     *
     * @param gameName DOCUMENT ME!
     * @param player1 DOCUMENT ME!
     * @param player2 DOCUMENT ME!
     * @param player3 DOCUMENT ME!
     * @param player4 DOCUMENT ME!
     * @param score1 DOCUMENT ME!
     * @param score2 DOCUMENT ME!
     * @param score3 DOCUMENT ME!
     * @param score4 DOCUMENT ME!
     * @param startTime DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws SQLException DOCUMENT ME!
     */
    public static boolean saveGameScores(String gameName, String player1, String player2, String player3, String player4, short score1, short score2, short score3, short score4, java.util.Date startTime) throws SQLException
    {
        // ensure that the JDBC connection is still valid
        if (connection == null)
        {
            return false;
        }

        try
        {
            // fill in the data values to the Prepared statement
            _preparedStatementForSaveGameCommand.setString(1, gameName);
            _preparedStatementForSaveGameCommand.setString(2, player1);
            _preparedStatementForSaveGameCommand.setString(3, player2);
            _preparedStatementForSaveGameCommand.setString(4, player3);
            _preparedStatementForSaveGameCommand.setString(5, player4);
            _preparedStatementForSaveGameCommand.setShort(6, score1);
            _preparedStatementForSaveGameCommand.setShort(7, score2);
            _preparedStatementForSaveGameCommand.setShort(8, score3);
            _preparedStatementForSaveGameCommand.setShort(9, score4);
            _preparedStatementForSaveGameCommand.setTimestamp(10, new Timestamp(startTime.getTime()));

            // execute the Command
            _preparedStatementForSaveGameCommand.executeQuery();
        }
        catch (SQLException sqlE)
        {
            sqlE.printStackTrace();
            throw sqlE;
        }

        return true;
    }

    /**
     * DOCUMENT ME!
     *
     * @param robotName DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws SQLException DOCUMENT ME!
     */
    public static SOCRobotParameters retrieveRobotParams(String robotName) throws SQLException
    {
        SOCRobotParameters robotParams = null;

        // ensure that the JDBC connection is still valid
        if (connection == null)
        {
            return robotParams;
        }

        try
        {
            // fill in the data values to the Prepared statement
            _preparedStatementForRobotParamsQuery.setString(1, robotName);

            // execute the Query
            ResultSet resultSet = _preparedStatementForRobotParamsQuery.executeQuery();

            if (!resultSet.next())
            {
                // the database has no results - therefore the user is not authenticated
                resultSet.close();

                return robotParams;
            }

            // retrieve the resultset
            int mgl = resultSet.getInt(2);
            int me = resultSet.getInt(3);
            float ebf = resultSet.getFloat(4);
            float af = resultSet.getFloat(5);
            float laf = resultSet.getFloat(6);
            float dcm = resultSet.getFloat(7);
            float tm = resultSet.getFloat(8);
            int st = resultSet.getInt(9);
            int tf = resultSet.getInt(14);
            resultSet.close();
            robotParams = new SOCRobotParameters(mgl, me, ebf, af, laf, dcm, tm, st, tf);
        }
        catch (SQLException sqlE)
        {
            sqlE.printStackTrace();
            throw sqlE;
        }

        return robotParams;
    }

    /**
     * DOCUMENT ME!
     */
    public static void cleanup()
    {
    	try
    	{
	        try
	        {
	            _preparedStatementForCreateAccountCommand.close();
	            _preparedStatementForUserPasswordQuery.close();
	            _preparedStatementForHostQuery.close();
	            _preparedStatementForLastloginUpdate.close();
	            _preparedStatementForSaveGameCommand.close();
	            _preparedStatementForRobotParamsQuery.close();
	            connection.close();
	        }
	        catch (SQLException sqlE)
	        {
	            sqlE.printStackTrace();
	        }
    	}
    	catch (Exception e) {}
    }

    //-------------------------------------------------------------------
    // dispResultSet
    // Displays all columns and rows in the given result set
    //-------------------------------------------------------------------
    private static void dispResultSet(ResultSet rs) throws SQLException
    {
        System.out.println("dispResultSet()");

        int i;

        // Get the ResultSetMetaData.  This will be used for
        // the column headings
        ResultSetMetaData rsmd = rs.getMetaData();

        // Get the number of columns in the result set
        int numCols = rsmd.getColumnCount();

        // Display column headings
        for (i = 1; i <= numCols; i++)
        {
            if (i > 1)
            {
                System.out.print(",");
            }

            System.out.print(rsmd.getColumnLabel(i));
        }

        System.out.println("");

        // Display data, fetching until end of the result set
        boolean more = rs.next();

        while (more)
        {
            // Loop through each column, getting the
            // column data and displaying
            for (i = 1; i <= numCols; i++)
            {
                if (i > 1)
                {
                    System.out.print(",");
                }

                System.out.print(rs.getString(i));
            }

            System.out.println("");

            // Fetch the next result set row
            more = rs.next();
        }
    }
}
