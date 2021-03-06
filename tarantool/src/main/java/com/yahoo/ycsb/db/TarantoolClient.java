package com.yahoo.ycsb.db;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Properties;

import org.tarantool.TarantoolConnection16;
import org.tarantool.TarantoolConnection16Impl;
import org.tarantool.TarantoolException;
import org.tarantool.CommunicationException;

import com.yahoo.ycsb.DB;
import com.yahoo.ycsb.DBException;
import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.StringByteIterator;

public class TarantoolClient extends DB{
	TarantoolConnection16Impl connection;
	
	private static final String HOST_PROPERTY       = "tnt.host";
	private static final String PORT_PROPERTY       = "tnt.port";
	private static final String SPACE_NO_PROPERTY   = "tnt.space.no";
	private static final String SPACE_NAME_PROPERTY = "tnt.space.name";
	private static final String USER_NAME_PROPERTY  = "tnt.user.name";
	private static final String USER_PASS_PROPERTY  = "tnt.user.pass";

	private static final String DEFAULT_HOST        = "build.tarantool.org";
	private static final String DEFAULT_PORT        = "33009";
	private static final String DEFAULT_CALL        = "false";
	private static final String DEFAULT_SPACE_NO    = "1024";
	private static final String DEFAULT_SPACE_NAME  = "ycsb";
	private static final String DEFAULT_USER_NAME   = "";
	private static final String DEFAULT_USER_PASS   = "";

	private String  spaceName;
	private int     spaceNo;
	private int     port;
	private String  host;
	private String  user;
	private String  passwd;
	
	private static final Logger log = Logger.getLogger(Tarantool16Client.class.getName());

	public void init() throws CommunicationException {

		Properties props = getProperties();
		this.host      = props.getProperty(HOST_PROPERTY);
		if (this.host == null) {
			this.host = DEFAULT_HOST;
		}
		String port = props.getProperty(PORT_PROPERTY);
		if (port == null) {
			port = DEFAULT_PORT;
		}
		this.port = Integer.parseInt(port);
		String spaceNo = props.getProperty(SPACE_NO_PROPERTY);
		if (spaceNo == null) {
			spaceNo = DEFAULT_SPACE_NO;
		}
		this.spaceNo = Integer.parseInt(spaceNo);
		this.spaceName      = props.getProperty(SPACE_NAME_PROPERTY);
		if (this.spaceName == null) {
			this.spaceName = DEFAULT_SPACE_NAME;
		}
		this.user      = props.getProperty(USER_NAME_PROPERTY, DEFAULT_USER_NAME);
		this.passwd    = props.getProperty(USER_PASS_PROPERTY, DEFAULT_USER_PASS);

		try {
			this.connection = new TarantoolConnection16Impl(this.host, this.port);
			if (this.user != "")
				this.connection.auth(this.user, this.passwd);
		} catch (CommunicationException e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public void cleanup() throws DBException{
		this.connection.close();
	}

	@Override
	public int insert(String table, String key, HashMap<String, ByteIterator> values) {
		int j = 0;
		String[] tuple = new String[1 + 2 * values.size()];
		tuple[0] = key;
		for (Map.Entry<String, ByteIterator> i: values.entrySet()) {
			tuple[j + 1] = i.getKey();
			tuple[j + 2] = i.getValue().toString();
			j += 2;
		}
		try {
            this.connection.replace(this.spaceNo, tuple);
		} catch (TarantoolException e) {
			e.printStackTrace();
			return 1;
		}
		return 0;
	}

	private HashMap<String, ByteIterator> tuple_convert_filter (List<String> input,
			Set<String> fields) {
		HashMap<String, ByteIterator> result = new HashMap<String, ByteIterator>();
		if (input == null)
			return result;
		for (int i = 1; i < input.toArray().length; i += 2)
			if (fields == null || fields.contains(input.get(i)))
				result.put(input.get(i), new StringByteIterator(input.get(i+1)));
		return result;
	}

	@Override
	public int read(String table, String key, Set<String> fields,
			HashMap<String, ByteIterator> result) {
		try {
			List<String> response;
            response = this.connection.select(this.spaceNo, 0, Arrays.asList(key), 0, 1, 0);
			result = tuple_convert_filter(response, fields);
			return 0;
		} catch (TarantoolException e) {
			e.printStackTrace();
			return 1;
		} catch (IndexOutOfBoundsException e) {
			return 1;
		}
	}

	@Override
	public int scan(String table, String startkey,
			int recordcount, Set<String> fields,
			Vector<HashMap<String, ByteIterator>> result) {
		List<String> response;
		try {
            response = this.connection.select(this.spaceNo, 0, Arrays.asList(startkey), 0, recordcount, 6);
		} catch (TarantoolException e) {
			e.printStackTrace();
			return 1;
		}
		HashMap<String, ByteIterator> temp = tuple_convert_filter(response, fields);
		if (!temp.isEmpty())
			result.add((HashMap<String, ByteIterator>) temp.clone());
		return 0;
	}

	@Override
	public int delete(String table, String key) {
		try {
            this.connection.delete(this.spaceNo, Arrays.asList(key));
		} catch (TarantoolException e) {
			e.printStackTrace();
			return 1;
		} catch (IndexOutOfBoundsException e) {
			return 1;
		}
		return 0;
	}
	@Override
	public int update(String table, String key,
			HashMap<String, ByteIterator> values) {
		int j = 0;
		String[] tuple = new String[1 + 2 * values.size()];
		tuple[0] = key;
		for (Map.Entry<String, ByteIterator> i: values.entrySet()) {
			tuple[j + 1] = i.getKey();
			tuple[j + 2] = i.getValue().toString();
			j += 2;
		}
		try {
            this.connection.replace(this.spaceNo, tuple);
		} catch (TarantoolException e) {
			e.printStackTrace();
			return 1;
		}
		return 0;

	}
}
