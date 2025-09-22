package com.endor;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;

//@WebServlet(name = "FileUploadServlet", urlPatterns = {"/upload"})
@MultipartConfig
public class FileUploadServlet extends HttpServlet {

    private final static Logger LOGGER =
            Logger.getLogger(FileUploadServlet.class.getCanonicalName());

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }
        protected void processRequest(HttpServletRequest request,
                                  HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        // Create path components to save the file

            System.out.println("Came to processRequest1 ");
        final String requestedPath = request.getParameter("destination");
            System.out.println("Came to processRequest2");

            final Part filePart = request.getPart("file");
            System.out.println("Came to processRequest3");

            final String rawFileName = getFileName(filePart);
            System.out.println("Came to processRequest4");
            
            // SECURITY: Validate and sanitize the path and filename to prevent path traversal
            final String validatedPath = validateAndSanitizePath(requestedPath);
            final String sanitizedFileName = sanitizeFileName(rawFileName);
            
            if (validatedPath == null || sanitizedFileName == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().println("Invalid file path or filename provided.");
                return;
            }


            OutputStream out = null;
        InputStream filecontent = null;
        final PrintWriter writer = response.getWriter();

        try {
            // Use the validated path and sanitized filename
            File targetFile = new File(validatedPath, sanitizedFileName);
            
            // Additional security check: ensure the final path is within the allowed directory
            if (!isFileWithinAllowedDirectory(targetFile, validatedPath)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().println("Invalid file path - path traversal detected.");
                return;
            }
            
            out = new FileOutputStream(targetFile);
            filecontent = filePart.getInputStream();

            int read = 0;
            final byte[] bytes = new byte[1024];

            while ((read = filecontent.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            writer.println("New file " + sanitizedFileName + " created at " + validatedPath);
            LOGGER.log(Level.INFO, "File {0} being uploaded to {1}",
                    new Object[]{sanitizedFileName, validatedPath});
        } catch (FileNotFoundException fne) {
            writer.println("You either did not specify a file to upload or are "
                    + "trying to upload a file to a protected or nonexistent "
                    + "location.");
            writer.println("<br/> ERROR: " + fne.getMessage());

            LOGGER.log(Level.SEVERE, "Problems during file upload. Error: {0}",
                    new Object[]{fne.getMessage()});
        } finally {
            if (out != null) {
                out.close();
            }
            if (filecontent != null) {
                filecontent.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
    }

    private String getFileName(final Part part) {
        final String partHeader = part.getHeader("content-disposition");
        LOGGER.log(Level.INFO, "Part Header = {0}", partHeader);
        for (String content : part.getHeader("content-disposition").split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(
                        content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }
    
    /**
     * Validates and sanitizes the destination path to prevent path traversal attacks
     */
    private String validateAndSanitizePath(String requestedPath) {
        if (requestedPath == null || requestedPath.trim().isEmpty()) {
            return null;
        }
        
        // Remove any path traversal sequences
        String sanitized = requestedPath.replaceAll("\\.\\.[\\\\/]", "")
                                      .replaceAll("[\\\\/]\\.\\.[\\\\/]", "")
                                      .replaceAll("[\\\\/]\\.\\.", "");
        
        // Define allowed upload directory (configure as needed)
        String allowedUploadDir = System.getProperty("user.home") + File.separator + "uploads";
        
        try {
            Path requestedPathObj = Paths.get(sanitized).normalize();
            Path allowedPathObj = Paths.get(allowedUploadDir).normalize();
            
            // Ensure the requested path starts with the allowed directory
            if (!requestedPathObj.startsWith(allowedPathObj)) {
                // If not within allowed directory, default to the allowed directory
                return allowedUploadDir;
            }
            
            return requestedPathObj.toString();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Invalid path provided: " + requestedPath, e);
            return null;
        }
    }
    
    /**
     * Sanitizes the filename to prevent path traversal and other security issues
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return null;
        }
        
        // Use Apache Commons IO to get just the filename without path components
        String sanitized = FilenameUtils.getName(fileName);
        
        // Additional sanitization: remove any remaining dangerous characters
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Ensure filename is not empty after sanitization
        if (sanitized.trim().isEmpty()) {
            return "uploaded_file_" + System.currentTimeMillis();
        }
        
        return sanitized;
    }
    
    /**
     * Checks if the target file is within the allowed directory
     */
    private boolean isFileWithinAllowedDirectory(File targetFile, String allowedDirectory) {
        try {
            Path targetPath = targetFile.getCanonicalFile().toPath();
            Path allowedPath = Paths.get(allowedDirectory).toRealPath();
            
            return targetPath.startsWith(allowedPath);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error checking file path security", e);
            return false;
        }
    }
}
