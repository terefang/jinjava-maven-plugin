package com.github.terefang.jinjava_maven_plugin.util;

import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;
import org.hjson.JsonValue;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.*;

public class ContextUtil {

    @SneakyThrows
    public static Map<String, Object> loadContextFrom(File _file) throws MojoExecutionException
    {
        Map<String, Object> _ret = new HashMap<>();
        FileReader _fh = new FileReader(_file);
        if(_file.getName().endsWith(".yaml")
            || _file.getName().endsWith(".yml"))
        {
            _ret.putAll(loadContextFromYaml(_fh));
        }
        else
        if(_file.getName().endsWith(".json")
                || _file.getName().endsWith(".hson")
                || _file.getName().endsWith(".hjson"))
        {
            _ret.putAll(loadContextFromHjson(_fh));
        }
        else
        if(_file.getName().endsWith(".properties"))
        {
            _ret.putAll(loadContextFromProperties(_fh));
        }
        else
        {
            throw new MojoExecutionException(MessageFormat.format("Context file '{0}' is unknown format", _file.getName()));
        }
        IOUtil.close(_fh);
        return _ret;
    }

    @SneakyThrows
    public static Map<String, Object> loadContextFromProperties(Reader _source)
    {
        Properties _prop = new Properties();
        _prop.load(_source);
        return (Map)_prop;
    }

    @SneakyThrows
    public static Map<String, Object> loadContextFromYaml(Reader _source)
    {
        Yaml _y = new Yaml();
        HashMap<String, Object> _obj = _y.loadAs(_source, HashMap.class);
        return _obj;
    }

    @SneakyThrows
    public static Map<String, Object> loadContextFromHjson(Reader _source)
    {
        HashMap<String, Object> _obj = new HashMap<>();
        JsonValue _hson = JsonValue.readHjson(_source);
        for(Map.Entry<String, Object> _entry : hjsonToMap(_hson).entrySet())
        {
            _obj.put(_entry.getKey(), _entry.getValue());
        }
        return _obj;
    }

    static Map<String, Object> hjsonToMap(JsonValue _v)
    {
        Map<String, Object> _ret = new HashMap<>();
        _v.asObject().forEach(m -> _ret.put(m.getName(), hjsonToValue(m.getValue())));
        return _ret;
    }

    static Object hjsonToValue(JsonValue value)
    {
        if(value.isObject())
        {
            return hjsonToMap(value);
        }
        else
        if(value.isArray())
        {
            return hjsonToArray(value);
        }
        else
        if(value.isString())
        {
            return value.asString();
        }
        else
        if(value.isNumber())
        {
            return Double.valueOf(value.asDouble());
        }
        else
        if(value.isBoolean())
        {
            return Boolean.valueOf(value.asBoolean());
        }
        else
        if(value.isNull())
        {
            return null;
        }
        else
        {
            return value.toString();
        }
    }

    static List hjsonToArray(JsonValue value)
    {
        List _ret = new Vector();
        value.asArray().forEach(m -> _ret.add(hjsonToValue(m)));
        return _ret;
    }
}
