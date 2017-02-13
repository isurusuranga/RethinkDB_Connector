package org.wso2.dss.connectors.rethinkdb;

import com.rethinkdb.model.MapObject;
import com.rethinkdb.net.Connection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.dss.connectors.rethinkdb.Constants.RethinkDBOperation;
import org.wso2.dss.connectors.rethinkdb.Constants.RethinkDBOperationLabels;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.dataservices.core.DBUtils;
import org.wso2.carbon.dataservices.core.DataServiceFault;
import org.wso2.carbon.dataservices.core.custom.datasource.*;
import org.wso2.carbon.dataservices.core.engine.InternalParam;

import java.util.*;

import com.rethinkdb.RethinkDB;

/**
 * The RethinkDB custom data source implementation for WSO2 DSS.
 */
public class RethinkDBDataSource implements CustomQueryBasedDS {
	private static final Log log = LogFactory.getLog(RethinkDBDataSource.class);
	private static final RethinkDB r = RethinkDB.r;
	private Connection connection;

	public QueryResult executeQuery(String query, List<InternalParam> params)
			throws DataServiceFault {
		return new RethinkQueryResult(query, params);
	}

	public void init(Map<String, String> map) throws DataServiceFault {
		String hostName = map.get(Constants.SERVER);
		if (DBUtils.isEmptyString(hostName)) {
			throw new DataServiceFault("The data source param '" +
			                           Constants.SERVER + "' is required");
		}
		String port = map.get(Constants.PORT);
		if (DBUtils.isEmptyString(port)) {
			throw new DataServiceFault("The data source param '" +
			                           Constants.PORT + "' is required");
		}
		int serverPort = Integer.parseInt(port);
		String database = map.get(Constants.DATABASE);
		if (DBUtils.isEmptyString(database)) {
			throw new DataServiceFault("The data source param '" +
			                           Constants.DATABASE + "' is required");
		}
		String timeout = map.get(Constants.TIME_OUT);
		if (DBUtils.isEmptyString(timeout)) {
			throw new DataServiceFault("The data source param '" +
			                           Constants.TIME_OUT + "' is required");
		}
		long serverTimeout = Long.parseLong(timeout);
		this.connection = r.connection().hostname(hostName).port(serverPort).db(database)
		                   .timeout(serverTimeout).connect();
	}

	public void close() {
		connection.close(false);
	}

	private Object[] decodeQuery(String query) throws DataServiceFault {
		int i1 = query.indexOf('.');
		if (i1 == -1) {
			throw new DataServiceFault("The RethinkDB table not specified in the query '" +
			                           query + "'");
		}
		String tableName = query.substring(0, i1).trim();
		int i2 = query.indexOf('(', i1);
		if (i2 == -1 || i2 - i1 <= 1) {
			throw new DataServiceFault("Invalid RethinkDB operation in the query '" +
			                           query + "'");
		}
		String operation = query.substring(i1 + 1, i2).trim();
		int i3 = query.lastIndexOf(')');
		if (i3 == -1) {
			throw new DataServiceFault("Invalid RethinkDB operation in the query '" +
			                           query + "'");
		}
		String opQuery = null;
		if (i3 - i2 > 1) {
			opQuery = query.substring(i2 + 1, i3).trim();
		}
		RethinkDBOperation rethinkOp = this.convertToRethinkOp(operation);

		return new Object[] { tableName, rethinkOp,
		                      this.checkAndCleanOpQuery(opQuery) };
	}

	private String checkAndCleanOpQuery(String opQuery) throws DataServiceFault {
		if (opQuery == null) {
			return null;
		}
		int a = 0, b = 0;
		if (opQuery.startsWith("'") || opQuery.startsWith("\"")) {
			a = 1;
		}
		if (opQuery.endsWith("'") || opQuery.endsWith("\"")) {
			b = 1;
		}
		return opQuery.substring(a, opQuery.length() - b);
	}

	private RethinkDBOperation convertToRethinkOp(String operation) throws DataServiceFault {
		if (RethinkDBOperationLabels.COUNT.equals(operation)) {
			return RethinkDBOperation.COUNT;
		} else if (RethinkDBOperationLabels.DELETE.equals(operation)) {
			return RethinkDBOperation.DELETE;
		} else if (RethinkDBOperationLabels.FILTER.equals(operation)) {
			return RethinkDBOperation.FILTER;
		} else if (RethinkDBOperationLabels.INSERT.equals(operation)) {
			return RethinkDBOperation.INSERT;
		} else if (RethinkDBOperationLabels.UPDATE.equals(operation)) {
			return RethinkDBOperation.UPDATE;
		} else if (RethinkDBOperationLabels.ORDER_BY.equals(operation)) {
			return RethinkDBOperation.ORDER_BY;
		} else if (RethinkDBOperationLabels.PLUCK.equals(operation)) {
			return RethinkDBOperation.PLUCK;
		} else if (RethinkDBOperationLabels.DROP.equals(operation)) {
			return RethinkDBOperation.DROP;
		} else if (RethinkDBOperationLabels.RETRIEVE_ALL.equals(operation)) {
			return RethinkDBOperation.RETRIEVE_ALL;
		} else if (RethinkDBOperationLabels.CREATE.equals(operation)) {
			return RethinkDBOperation.CREATE;
		} else {
			throw new DataServiceFault("Unknown RethinkDB operation '" + operation + "'");
		}
	}

	private MapObject getMapObject(String opQuery, Object[] parameters) throws JSONException {
		MapObject mapObject = null;
		JSONObject jsonQuery = new JSONObject(opQuery);
		Iterator keysToCopyIterator = jsonQuery.keys();
		List<String> keysList = new ArrayList<String>();
		while (keysToCopyIterator.hasNext()) {
			String key = (String) keysToCopyIterator.next();
			keysList.add(key);
		}
		String[] keysArray = keysList.toArray(new String[keysList.size()]);
		if (keysArray.length == parameters.length) {
			for (int i = 0; i < keysArray.length; i++) {
				int j = jsonQuery.getInt(keysArray[i]) - 1;
				if (jsonQuery.getInt(keysArray[i]) == 1) {
					mapObject = r.hashMap(keysArray[i], parameters[j]);
					break;
				}
			}
			for (int k = 0; k < keysArray.length; k++) {
				int m = jsonQuery.getInt(keysArray[k]) - 1;
				if (jsonQuery.getInt(keysArray[k]) == 1) {
					continue;
				} else {
					if (mapObject != null) {
						mapObject.with(keysArray[k], parameters[m]);
					}
				}
			}
		}
		return mapObject;
	}

	public class RethinkQueryResult implements QueryResult {

		private Iterator<? extends Object> dataIterator;

		public RethinkQueryResult(String query, List<InternalParam> params)
				throws DataServiceFault {
			Object[] request = decodeQuery(query);
			String tableName = (String) request[0];
			String opQuery = (String) request[2];
			Object[] rethinkParams = DBUtils.convertInputParamValues(params);
			switch ((RethinkDBOperation) request[1]) {
				case COUNT:
					this.dataIterator = this.doCount(tableName);
					break;
				case FILTER:
					this.dataIterator = this.doFilter(tableName, opQuery, rethinkParams);
					break;
				case DELETE:
					this.doDeleteAllRecords(tableName);
					break;
				case INSERT:
					this.doInsert(tableName, opQuery, rethinkParams);
					break;
				case ORDER_BY:
					this.dataIterator = this.doOrderBy(tableName, opQuery);
					break;
				case UPDATE:
					this.doUpdate(tableName, opQuery, rethinkParams);
					break;
				case PLUCK:
					this.dataIterator = this.doPluck(tableName, opQuery);
					break;
				case DROP:
					this.doDropTable(tableName);
					break;
				case RETRIEVE_ALL:
					this.dataIterator = this.doRetrieveAllRecords(tableName);
					break;
				case CREATE:
					this.doCreate(tableName);
					break;
			}
		}

		private Iterator<Long> doCount(String tableName) {
			long count = r.table(tableName).count().run(connection);
			List<Long> countResult = new ArrayList<Long>();
			countResult.add(count);
			return countResult.iterator();
		}

		private void doInsert(String tableName, String opQuery, Object[] parameters)
				throws DataServiceFault {
			if (opQuery != null) {
				if (parameters.length > 0) {
					try {
						MapObject object = getMapObject(opQuery, parameters);
						if (object != null) {
							r.table(tableName).insert(object).run(connection);
						} else {
							throw new DataServiceFault("Rethink empty insert statement");
						}
					} catch (JSONException e) {
						log.error("JSON exception occurred while doing insert operation", e);
					}
				} else {
					throw new DataServiceFault("Rethink insert statements must contain parameters");
				}
			} else {
				throw new DataServiceFault("Rethink insert statements must contain a query");
			}
		}

		private Iterator<String> doFilter(String tableName, String opQuery, Object[] parameters)
				throws DataServiceFault {
			Iterator<String> filterIterator = null;
			if (opQuery != null) {
				if (parameters.length > 0) {
					try {
						MapObject object = getMapObject(opQuery, parameters);
						if (object != null) {
							filterIterator = r.table(tableName).filter(object).run(connection);
						} else {
							throw new DataServiceFault("Rethink empty filter statement");
						}
					} catch (JSONException e) {
						log.error("JSON exception occurred while doing filter operation", e);
					}
				} else {
					throw new DataServiceFault("Rethink filter statements must contain parameters");
				}
			} else {
				throw new DataServiceFault("Rethink filter statements must contain a query");
			}
			return filterIterator;
		}

		private void doUpdate(String tableName, String opQuery, Object[] parameters)
				throws DataServiceFault {
			if (opQuery != null) {
				if (parameters.length > 0) {
					try {
						MapObject object = getMapObject(opQuery, parameters);
						if (object != null) {
							r.table(tableName).update(object).run(connection);
						} else {
							throw new DataServiceFault("Rethink empty update statement");
						}
					} catch (JSONException e) {
						log.error("JSON exception occurred while doing update operation", e);
					}
				} else {
					throw new DataServiceFault("Rethink update statements must contain parameters");
				}
			} else {
				throw new DataServiceFault("Rethink update statements must contain a query");
			}
		}

		private Iterator<String> doOrderBy(String tableName, String opQuery) {
			List<String> orderByResult = r.table(tableName).orderBy(opQuery).run(connection);
			return orderByResult.iterator();
		}

		private Iterator<String> doPluck(String tableName, String opQuery) {
			String[] opQueryArray = opQuery.trim().split(",");
			return r.table(tableName).pluck(opQueryArray).run(connection);
		}

		private Iterator<String> doRetrieveAllRecords(String tableName) {
			return r.table(tableName).run(connection);
		}

		public void doCreate(String tableName) throws DataServiceFault {
			if (!this.isTableExists(tableName)) {
				r.tableCreate(tableName).run(connection);
			} else {
				throw new DataServiceFault("Rethink create statement - table already exists");
			}
		}

		private void doDeleteAllRecords(String tableName) {
			r.table(tableName).delete().run(connection);
		}

		private void doDropTable(String tableName) throws DataServiceFault {
			if (this.isTableExists(tableName)) {
				r.tableDrop(tableName).run(connection);
			} else {
				throw new DataServiceFault(
						"Rethink drop statements must contain a valid table name");
			}
		}

		private boolean isTableExists(String tableName) {
			if (r.tableList().contains(tableName).run(connection)) {
				return true;
			}
			return false;
		}

		public List<DataColumn> getDataColumns() throws DataServiceFault {
			List<DataColumn> result = new ArrayList<DataColumn>();
			result.add(new DataColumn(Constants.RESULT_COLUMN_NAME));
			return result;
		}

		public boolean hasNext() throws DataServiceFault {
			return this.dataIterator != null && this.dataIterator.hasNext();
		}

		public DataRow next() throws DataServiceFault {
			if (this.dataIterator == null) {
				throw new DataServiceFault("No Rethink data result available");
			} else {
				Object data = this.dataIterator.next();
				Map<String, String> values = new HashMap<String, String>();
				values.put(Constants.RESULT_COLUMN_NAME, data.toString());
				return new FixedDataRow(values);
			}
		}
	}

}
