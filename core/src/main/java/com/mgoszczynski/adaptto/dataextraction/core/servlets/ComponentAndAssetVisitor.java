package com.mgoszczynski.adaptto.dataextraction.core.servlets;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.AbstractResourceVisitor;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationStatus;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.Template;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.api.components.ComponentManager;
import com.google.gson.JsonObject;

public class ComponentAndAssetVisitor extends AbstractResourceVisitor {


    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentAndAssetVisitor.class);

    private final OutputStream os;
    private final ResourceResolver resourceResolver;
    private final PageManager pageManager;
    private final ComponentManager componentManager;


    public ComponentAndAssetVisitor(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        os = response.getOutputStream();
        resourceResolver = request.getResourceResolver();
        pageManager = resourceResolver.adaptTo(PageManager.class);
        componentManager = resourceResolver.adaptTo(ComponentManager.class);
    }

    @Override
    protected void visit(Resource resource) {
        try {
            outputAssetReferences(resource);
            outputComponentReferences(resource);
        } catch (Exception e) {
            LOGGER.error("Unknown exception on resource : {}", getResourcePathSafely(resource), e);
            throw new RuntimeException(e);
        }
    }

    private void outputComponentReferences(Resource resource) throws IOException {
        JsonObject currentResourceData = new JsonObject();
        String resourceType = resource.getValueMap().get(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, String.class);
        if (isNotBlank(resourceType) && !ResourceUtil.isSyntheticResource(resource) && !ResourceUtil.isNonExistingResource(resource)) {
            currentResourceData.addProperty("resourceType", resourceType );
            currentResourceData.addProperty("path", resource.getPath());
            addPageData(resource, currentResourceData);
            addComponentData(resource,currentResourceData);
            IOUtils.write(currentResourceData.toString() + "\n", os, "UTF-8");
        }
    }

    private void outputAssetReferences(Resource resource) throws IOException {
        String resourcePath = resource.getPath();
        List<String> assets = getAssets(resource);
        for (String asset : assets) {
            if (StringUtils.isNotBlank(asset)) {
                JsonObject currentAssetData = new JsonObject();
                currentAssetData.addProperty("path", resourcePath);
                currentAssetData.addProperty("fileReference", asset);
                addPageData(resource, currentAssetData);
                IOUtils.write(currentAssetData.toString() + "\n", os, "UTF-8");
            }
        }
    }

    private List<String> getAssets(Resource resource) {
        List<String> references = new LinkedList<String>();
        ValueMap valueMap = resource.getValueMap();
        for (Object value : valueMap.values()) {
            if (value instanceof String) {
                if (value.toString().startsWith("/content/dam")) {
                    references.add(value.toString());
                }
            }
            if (value instanceof String[]) {
                String[] values = (String[]) value;
                for (String s : values) {
                    if (s.startsWith("/content/dam")) {
                        references.add(value.toString());
                    }
                }
            }
        }
        return references;
    }

    private String getResourcePathSafely(Resource resource) {
        if (resource != null) {
            return resource.getPath();
        }
        return StringUtils.EMPTY;
    }

    private void addComponentData(Resource resource, JsonObject resourceJson) {
        JsonObject componentData = new JsonObject();
        Component currentComponent = componentManager.getComponentOfResource(resource);
        if (currentComponent != null) {
            componentData.addProperty("path", currentComponent.getPath());
            componentData.addProperty("superType", currentComponent.getPath());
            componentData.addProperty("group", currentComponent.getComponentGroup());
            componentData.addProperty("title", currentComponent.getTitle());
            Component superComponent = currentComponent.getSuperComponent();
            if (superComponent != null) {
                componentData.addProperty("superType", superComponent.getPath());
            }
            resourceJson.add("component", componentData);
        }
    }

    private void addPageData(Resource resource, JsonObject resourceJson) {
        if (pageManager != null) {
            Page containingPage = pageManager.getContainingPage(resource);
            if (containingPage != null) {
                JsonObject pageJson = new JsonObject();

                pageJson.addProperty("path", containingPage.getPath());
                pageJson.addProperty("template", getTemplate(containingPage));
                pageJson.addProperty("pageLastModified", formatDate(containingPage.getLastModified()));

                ReplicationStatus replicationStatus = containingPage.getContentResource().adaptTo(ReplicationStatus.class);
                if (replicationStatus != null) {
                    pageJson.addProperty("isActivated", replicationStatus.isActivated());
                    pageJson.addProperty("lastReplicationAction", getReplicationAction(replicationStatus));
                    pageJson.addProperty("lastPublished", formatDate(replicationStatus.getLastPublished()));
                }
                resourceJson.add("page", pageJson);
            }
        }

    }

    private String getTemplate(Page containingPage) {
        if (containingPage != null) {
            Template template = containingPage.getTemplate();
            if (template != null) {
                return template.getPath();
            } else {
                if (containingPage.getProperties().containsKey("cq:Template")) {
                    return containingPage.getProperties().get("cq:Template", String.class);
                }
            }
        }
        return "";
    }

    private String getReplicationAction(ReplicationStatus replicationStatus) {
        ReplicationActionType lastReplicationAction = replicationStatus.getLastReplicationAction();
        return lastReplicationAction != null ? lastReplicationAction.getName() : StringUtils.EMPTY;
    }

    private String formatDate(Calendar date) {
        if (date != null) {
            return FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSSS").format(date.getTime());
        }
        return StringUtils.EMPTY;
    }
}
