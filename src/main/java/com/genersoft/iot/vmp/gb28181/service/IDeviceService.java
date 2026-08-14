package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Equipment related business processing
 * @author lin
 */
public interface IDeviceService {

    /**
     * Device online
     * @param device Device information
     */
    void online(Device device);

    /**
     * Equipment offline
     */
    void offline(Device device);

    /**
     * Add directory subscription
     * @param device Device information
     * @return Boolean
     */
    boolean addCatalogSubscribe(Device device, SipTransactionInfo transactionInfo);

    /**
     * Remove directory subscription
     * @param device Device information
     * @return Boolean
     */
    boolean removeCatalogSubscribe(Device device, CommonCallback<Boolean> callback);

    /**
     * Add mobile location subscription
     * @param device Device information
     * @return Boolean
     */
    boolean addMobilePositionSubscribe(Device device, SipTransactionInfo transactionInfo);

    /**
     * Remove mobile location subscription
     * @param device Device information
     * @return Boolean
     */
    boolean removeMobilePositionSubscribe(Device device, CommonCallback<Boolean> callback);

    boolean addAlarmSubscribe(@NotNull Device device, SipTransactionInfo transactionInfo);

    boolean removeAlarmSubscribe(Device device, CommonCallback<Boolean> callback);

    /**
     * Remove mobile location subscription
     * @param deviceId EquipmentID
     * @return Sync status
     */
    SyncStatus getChannelSyncStatus(String deviceId);

    /**
     * Check to see if it's still syncing
     * @param deviceId EquipmentID
     * @return Boolean
     */
    Boolean isSyncRunning(String deviceId);

    /**
     * Channel synchronization
     * @param device Device information
     */
    void sync(Device device);

    /**
     * Query device information
     * @param deviceId Device number
     * @return Device information
     */
    Device getDeviceByDeviceId(String deviceId);

    /**
     * Get all online devices
     * @return Device list
     */
    List<Device> getAllOnlineDevice(String serverId);

    List<Device> getAllByStatus(Boolean status);

    /**
     * Check device status
     * @param device Device information
     */
    Boolean getDeviceStatus(Device device);

    /**
     * Get device information based on IP and port
     * @param host IP
     * @param port port
     * @return Device information
     */
    Device getDeviceByHostAndPort(String host, int port);

    /**
     * Update device
     * @param device Device information
     */
    void updateDevice(Device device);

    @Transactional
    void updateDeviceList(List<Device> deviceList);

    /**
     * Check if the device number already exists
     * @param deviceId Device number
     * @return
     */
    boolean isExist(String deviceId);

    /**
     * Add device
     * @param device
     */
    void addCustomDevice(Device device);

    /**
     * Page form updates device information
     * @param device
     */
    void updateCustomDevice(Device device);

    /**
     * Remove device
     * @param deviceId
     * @return
     */
    boolean delete(String deviceId);

    /**
     * Get statistics
     * @return
     */
    ResourceBaseInfo getOverview();

    /**
     * Get all devices
     */
    List<Device> getAll();

    PageInfo<Device> getAll(int page, int count, String query, Boolean status);

    Device getDevice(Integer gbDeviceDbId);

    Device getDeviceByChannelId(Integer channelId);

    Device getDeviceBySourceChannelDeviceId(String requesterId);

    void subscribeCatalog(int id, int cycle);

    void subscribeMobilePosition(int id, int cycle, int interval);

    WVPResult<SyncStatus> devicesSync(Device device);

    void deviceBasicConfig(Device device, BasicParam basicParam, ErrorCallback<String> callback);

    void deviceVideoParamConfig(Device device, VideoParamOpt videoParamOpt, ErrorCallback<String> callback);

    <T extends DeviceConfigAware> void deviceConfigQuery(Device device, String channelId, Class<T> configClass, ErrorCallback<T> callback);

    void teleboot(Device device);

    void record(Device device, String channelId, String recordCmdStr, ErrorCallback<String> callback);

    void guard(Device device, String guardCmdStr, ErrorCallback<String> callback);

    void resetAlarm(Device device, String channelId, String alarmMethod, String alarmType, ErrorCallback<String> callback);

    void iFrame(Device device, String channelId);

    void homePosition(Device device, String channelId, Boolean enabled, Integer resetTime, Integer presetIndex, ErrorCallback<String> callback);

    void dragZoomIn(Device device, String channelId, int length, int width, int midPointX, int midPointY, int lengthX, int lengthY);

    void dragZoomOut(Device device, String channelId, int length, int width, int midPointX, int midPointY, int lengthX, int lengthY);

    void deviceStatus(Device device, ErrorCallback<String> callback);

    void subscribeAlarm(int id, int cycle);

    void updateDeviceHeartInfo(Device device);

    void alarm(Device device, String startPriority, String endPriority, String alarmMethod, String alarmType, String startTime, String endTime, ErrorCallback<Object> callback);

    void deviceInfo(Device device, ErrorCallback<Object> callback);

    void queryPreset(Device device, String channelId, ErrorCallback<List<Preset>> callback);

    List<TimeStatistics> getKeepaliveTimeStatistics(String deviceId, Integer count);

    List<TimeStatistics> getRegisterTimeStatistics(String deviceId, Integer count);
}
