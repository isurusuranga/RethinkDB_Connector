package org.wso2.dss.connectors.rethinkdb;

/**
 * Constants for rethinkDB
 */
public class Constants {
	public static final String SERVER = "rethinkDB_server";
	public static final String DATABASE = "rethinkDB_database";
	public static final String PORT = "rethinkDB_port";
	public static final String TIME_OUT = "rethinkDB_timeout";
	public static final String RESULT_COLUMN_NAME = "document";

	public static class RethinkDBOperationLabels {
		public static final String CREATE = "tableCreate";
		public static final String COUNT = "count";
		public static final String DELETE = "delete";
		public static final String FILTER = "filter";
		public static final String INSERT = "insert";
		public static final String UPDATE = "update";
		public static final String ORDER_BY = "orderBy";
		public static final String PLUCK = "pluck";
		public static final String DROP = "tableDrop";
		public static final String RETRIEVE_ALL = "table";
	}

	public static enum RethinkDBOperation {
		CREATE,
		COUNT,
		DELETE,
		FILTER,
		INSERT,
		UPDATE,
		ORDER_BY,
		PLUCK,
		DROP,
		RETRIEVE_ALL
	}
}
