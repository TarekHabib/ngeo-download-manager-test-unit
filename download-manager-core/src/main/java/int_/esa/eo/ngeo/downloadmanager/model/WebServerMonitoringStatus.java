package int_.esa.eo.ngeo.downloadmanager.model;

public class WebServerMonitoringStatus {
    private boolean darMonitoringRunning;

    public WebServerMonitoringStatus() {
        darMonitoringRunning = true;
    }

    public boolean isDarMonitoringRunning() {
        return darMonitoringRunning;
    }

    public void setDarMonitoringRunning(boolean darMonitoringRunning) {
        this.darMonitoringRunning = darMonitoringRunning;
    }
}
