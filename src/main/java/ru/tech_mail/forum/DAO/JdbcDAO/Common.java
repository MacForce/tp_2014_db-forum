package ru.tech_mail.forum.DAO.JdbcDAO;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.HtmlUtils;
import ru.tech_mail.forum.exceptions.WrongDataException;
import ru.tech_mail.forum.responses.BaseResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class Common {
    private static final Logger LOG = LoggerFactory.getLogger(BaseResponse.class);
    private static final ObjectMapper JSONMapper = new ObjectMapper();
    {
        JSONMapper.setVisibilityChecker(JSONMapper.getSerializationConfig().getDefaultVisibilityChecker()
            .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        JSONMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static String escapeInjections(String value) {
        // HTML transformation characters
        value = HtmlUtils.htmlEscape(value);
        // SQL injection characters
        value = StringEscapeUtils.escapeSql(value);
        return value;
    }

    public static void addToResponse(HttpServletResponse response, BaseResponse message) {
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(
                response.getOutputStream(), StandardCharsets.UTF_8), true)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Content-Type", "text/plain; charset=UTF-8");
            out.print(JSONMapper.writeValueAsString(message));
        } catch (IOException e) {
            LOG.error("Can't add string", e);
        }
    }

    public static void addStringToResponse(HttpServletResponse response, String message) {
        try (PrintWriter out = response.getWriter()) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Content-Type", "text/plain; charset=UTF-8");
            out.print(message);
        } catch (IOException e) {
            LOG.error("Can't add string", e);
        }
    }

    public static HashMap<String, Object> readRequest(HttpServletRequest request) {
        HashMap<String, Object> data = null;
        TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};
        try {
            data = JSONMapper.readValue(request.getReader(), typeRef);
        } catch (IOException e) {
            LOG.error("Can't parse request: " + request.getRequestURI(), e);
        }
        return data;
    }

    public static void addOK(HttpServletResponse response) {
        Common.addToResponse(response, new BaseResponse<>((byte) 0, "OK"));
    }

    public static void addNotFound(HttpServletResponse response) {
        Common.addToResponse(response, new BaseResponse<>((byte) 1, "not found data"));
    }

    public static void addNotValid(HttpServletResponse response) {
        Common.addToResponse(response, new BaseResponse<>((byte) 2, "not valid request"));
    }

    public static void addNotCorrect(HttpServletResponse response) {
        Common.addToResponse(response, new BaseResponse<>((byte) 3, "not correct request"));
    }

    public static void addError(HttpServletResponse response) {
        Common.addToResponse(response, new BaseResponse<>((byte) 4, "unknown error"));
    }

    public static void addExists(HttpServletResponse response) {
        Common.addToResponse(response, new BaseResponse<>((byte) 5, "data is already exists"));
    }
}
