
package com.mgoszczynski.adaptto.dataextraction.core.servlets;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Servlet.class)
@SlingServletPaths("/bin/data-extract.json")
public class DataExtractionServlet extends SlingAllMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataExtractionServlet.class);
    public static final String DEFAULT_EXTRACTION_PATH = "/content";



    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {

        try {
            String extractionPath = getExtractionPath(request);
            ResourceResolver resourceResolver = request.getResourceResolver();
            Resource rootResource = getResource(extractionPath, resourceResolver);
            LOGGER.info("Staring component usage analysis for {}", extractionPath);
            ComponentAndAssetVisitor clv = new ComponentAndAssetVisitor(request, response);
            clv.accept(rootResource);
            LOGGER.info("Finished component usage analysis");

        } catch (ResourceNotFoundException e) {
            LOGGER.error("resource not found");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception t) {
            //pass on all other exceptions.
            throw t;
        }

    }


    private Resource getResource(String searchPath, ResourceResolver resourceResolver) throws ResourceNotFoundException {
        Resource resource = resourceResolver.getResource(searchPath);
        if (resource != null && !ResourceUtil.isNonExistingResource(resource)) {
            return resource;
        } else {
            throw new ResourceNotFoundException(searchPath);
        }
    }


    private String getExtractionPath(SlingHttpServletRequest request) {
        RequestParameter path = request.getRequestParameter("path");
        if (path != null) {
            String string = path.getString();
            return StringUtils.defaultIfBlank(string, DEFAULT_EXTRACTION_PATH);
        }
        return DEFAULT_EXTRACTION_PATH;
    }
}
