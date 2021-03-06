package int_.esa.eo.ngeo.downloadmanager.observer;

import int_.esa.eo.ngeo.downloadmanager.model.ProductProgress;

public interface DownloadSubject {
    void registerObserver(DownloadObserver o);
    void notifyObserversOfProgress(String productUuid, ProductProgress productProgress);
    void notifyObserversOfProductDetails(String productUuid, String productName, Integer numberOfFiles, Long overallSize);
}
