package int_.esa.eo.ngeo.downloadmanager.webserver.builder;

import int_.esa.eo.ngeo.downloadmanager.exception.NonRecoverableException;
import int_.esa.eo.ngeo.downloadmanager.model.DataAccessRequest;
import int_.esa.eo.ngeo.downloadmanager.model.Product;
import int_.esa.eo.ngeo.downloadmanager.model.ProductProgress;
import int_.esa.eo.ngeo.downloadmanager.plugin.EDownloadStatus;
import int_.esa.eo.ngeo.iicd_d_ws._1.DMRegistrationMgmntRequ;
import int_.esa.eo.ngeo.iicd_d_ws._1.DataAccessMonitoringRequ;
import int_.esa.eo.ngeo.iicd_d_ws._1.MonitoringURLRequ;
import int_.esa.eo.ngeo.iicd_d_ws._1.ProductAccessStatus;
import int_.esa.eo.ngeo.iicd_d_ws._1.ProductAccessSubsetting;
import int_.esa.eo.ngeo.iicd_d_ws._1.ProductDownloadNotification;
import int_.esa.eo.ngeo.iicd_d_ws._1.ProductDownloadNotificationList;
import int_.esa.eo.ngeo.iicd_d_ws._1.ProductDownloadStatus;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NgeoWebServerRequestBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(NgeoWebServerRequestBuilder.class);

    public DMRegistrationMgmntRequ buildDMRegistrationMgmntRequest(String downloadManagerId, String downloadManagerFriendlyName) {
        DMRegistrationMgmntRequ dMRegistrationMgmntRequ = new DMRegistrationMgmntRequ();
        dMRegistrationMgmntRequ.setDownloadManagerId(downloadManagerId);
        dMRegistrationMgmntRequ.setDownloadManagerFriendlyName(downloadManagerFriendlyName);
        return dMRegistrationMgmntRequ;
    }

    public MonitoringURLRequ buildMonitoringURLRequest(String downloadManagerId, GregorianCalendar downloadManagerSetTime) {
        MonitoringURLRequ monitoringURLRequ = new MonitoringURLRequ();
        monitoringURLRequ.setDownloadManagerId(downloadManagerId);
        if(downloadManagerSetTime != null) {
            monitoringURLRequ.setDownloadManagerSetTime(buildXMLGregorianCalendar(downloadManagerSetTime));
        }
        return monitoringURLRequ;
    }

    public DataAccessMonitoringRequ buildDataAccessMonitoringRequ(String downloadManagerId, DataAccessRequest dataAccessRequest, ProductAccessStatus productAccessStatus) {
        DataAccessMonitoringRequ dataAccessMonitoringRequ = new DataAccessMonitoringRequ();
        dataAccessMonitoringRequ.setDownloadManagerId(downloadManagerId);
        ProductAccessSubsetting productAccessSubsetting = new ProductAccessSubsetting();
        Date lastResponseDate = dataAccessRequest.getLastResponseDate();
        if(lastResponseDate != null) {
            TimeZone utc = TimeZone.getTimeZone("UTC");
            GregorianCalendar calendar = new GregorianCalendar(utc);
            calendar.setTime(lastResponseDate);

            productAccessSubsetting.setDownloadManagerSetTime(buildXMLGregorianCalendar(calendar));
        }
        if(productAccessStatus == null) {
            productAccessStatus = ProductAccessStatus.READY_ACCESSED;
        }
        productAccessSubsetting.setReadyProductsOrAll(productAccessStatus);

        dataAccessMonitoringRequ.setProductAccessSubsetting(productAccessSubsetting);

        List<Product> productList = dataAccessRequest.getProductList();
        if(productList != null) {
            ProductDownloadNotificationList productDownloadNotificationList = buildProductDownloadNotificationList(productList);
            dataAccessMonitoringRequ.setProductDownloadNotificationList(productDownloadNotificationList);
        }

        return dataAccessMonitoringRequ;
    }

    private XMLGregorianCalendar buildXMLGregorianCalendar(GregorianCalendar gregorianCalendar) {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException e) {
            throw new NonRecoverableException("Unable to build monitoring URL request.", e);
        }
    }

    private ProductDownloadNotificationList buildProductDownloadNotificationList(List<Product> productList) {
        ProductDownloadNotificationList productDownloadNotificationList = new ProductDownloadNotificationList();
        List<ProductDownloadNotification> downloadNotificationList = productDownloadNotificationList.getProductDownloadNotifications();

        for (Product product : productList) {
            if(!product.isNotified()) {
                ProductDownloadNotification productDownloadNotification = buildProductDownloadNotification(product);
                if(productDownloadNotification != null) {
                    downloadNotificationList.add(productDownloadNotification);
                }
            }
        }

        return productDownloadNotificationList;
    }

    private ProductDownloadNotification buildProductDownloadNotification(Product product) {
        ProductProgress productProgress = product.getProductProgress();
        if(productProgress == null) {
            LOGGER.error("Product progress is empty for product, cannot include product into notification list.");
            return null;
        }

        ProductDownloadNotification productDownloadNotification = new ProductDownloadNotification();
        productDownloadNotification.setProductAccessURL(product.getProductAccessUrl());

        EDownloadStatus downloadStatus = productProgress.getStatus();
        if(downloadStatus == null) {
            LOGGER.error("Product status is empty for product, cannot include product into notification list.");
            return null;
        }

        ProductDownloadStatus productDownloadStatus;
        String productDownloadMessage;
        Long productDownloadProgress = null;

        switch (downloadStatus) {
        case NOT_STARTED:
            productDownloadStatus = ProductDownloadStatus.NOT_STARTED;
            productDownloadMessage = "Product download has not started.";
            break;
        case IDLE:
            productDownloadStatus = ProductDownloadStatus.NOT_STARTED;
            productDownloadMessage = "Product download has been requested, but product is not ready at this time.";
            break;
        case RUNNING:
            productDownloadStatus = ProductDownloadStatus.DOWNLOADING;
            productDownloadMessage = "Product download in progress.";
            productDownloadProgress = productProgress.getProgressPercentage();
            break;
        case PAUSED:
            productDownloadStatus = ProductDownloadStatus.DOWNLOADING;
            productDownloadMessage = "Product download paused.";
            productDownloadProgress = productProgress.getProgressPercentage();
            break;
        case CANCELLED:
            productDownloadStatus = ProductDownloadStatus.COMPLETED;
            productDownloadMessage = "Product cancelled.";
            productDownloadProgress = productProgress.getProgressPercentage();
            break;
        case IN_ERROR:
            productDownloadStatus = ProductDownloadStatus.ERROR;
            productDownloadMessage = String.format("Product download error: %s", productProgress.getMessage());
            break;
        case COMPLETED:
            productDownloadStatus = ProductDownloadStatus.COMPLETED;
            productDownloadMessage = "Product completed.";
            productDownloadProgress = productProgress.getProgressPercentage();
            break;
        default:
            LOGGER.error("Unable to determine product download status, cannot include product into notification list.");
            return null;
        }

        if(product.getProductProgress().getMessage() != null) {
            productDownloadMessage = product.getProductProgress().getMessage();
        }

        productDownloadNotification.setProductDownloadStatus(productDownloadStatus);
        productDownloadNotification.setProductDownloadMessage(productDownloadMessage);

        if(productDownloadProgress != null) {
            productDownloadNotification.setProductDownloadProgress(productDownloadProgress.intValue());
        }

        Long fileSize = product.getTotalFileSize();
        productDownloadNotification.setProductDownloadSize(fileSize);

        return productDownloadNotification;
    }
}
