/*
 * Copyright 2014-2015 The DevMine Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package ch.devmine.javaparser.utils;

import japa.parser.ast.expr.Expression;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class contains some utility methods to help the parser
 * 
 * @author lweingart
 */
public class ParserUtils {

    /**
     *
     * @param target
     *      a string representation of the right part of an assignment
     * @return
     *      a map with the string representation of the variable name
     *      and a string representation of the word 'this' if needed
     */
    public static Map<String, String> parseTarget(String target) {
        Map<String, String> result = new HashMap<>();
        int i = target.indexOf(".");
        if (i != -1) {
            result.put("this", target.substring(0, i));
            result.put("name", target.substring(i + 1));
        } else {
            result.put("this", null);
            result.put("name", target);
        }

        return result;
    }

    /**
     *
     * @param expr
     *      a string representation of an assignment
     * @return
     *      a string representation of the variable name
     */
    public static String extractValue(String expr) {
        String value = null;
        int i = expr.indexOf("=");
        if (i != -1) {
            boolean postSpace = expr.charAt(i + 1) == ' ';
            value = postSpace ? expr.substring(i + 2) : expr.substring(i + 1);
        }
        return value;
    }

    /**
     *
     * @param exception
     *      a string of an Exception declaration
     * @return
     *      a map with the type and the name of the exception
     */
    public static Map<String, String> parseException(String exception) {
        Map<String, String> result = new HashMap<>();
        int i = exception.indexOf(" ");
        String type = null;
        String name = null;
        if (i != -1) {
            type = exception.substring(0, i);
            name = exception.substring(i + 1);
        }
        result.put("type", type);
        result.put("name", name);

        return result;
    }

    /**
     *
     * @param expression
     *      an array initialisation (exemple : {{"a", "b"}, {"c", "d", "e"}}
     * @return
     *      an array of Integers with the representation of the dimensions
     */
    public static List<Integer> extractDimensions(List<Expression> expression) {
        List<Integer> result = new ArrayList<>();
        result.add(expression.size());
        for (Expression expr : expression) {
            if (expr instanceof List) {
                List l = (List) expr;
                result.addAll(extractDimensions(l));
            }
        }

        return result;
    }
}
