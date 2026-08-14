<!-- Electronic map -->

# electronic map

WVP provides a simple electronic map for device positioning and trajectory information of mobile devices. The electronic map is developed based on the open source map engine openlayers.

### View device location

1. You can click the "Locate" button in the device list to automatically jump to the electronic map page;
2. On the electronic map page, right-click "Position" on the device to obtain the location of all channels under the device/platform.
3. Click the channel information to locate the specific channel

### Query device track

To query the trajectory, you need to configure the save-position-history option in advance to enable the saving of trajectory information. Currently, WVP does not support sub-databases and tables. It is not competent for large amounts of trajectory information. If necessary, please develop secondary or customized development by yourself.
On the electronic map page, right-click "Query Track" on the device to obtain device track information.

PS: The current base map is only used for demonstration and learning. For commercial use, please purchase and authorize it yourself.

### Change basemap and basemap configuration

Currently, WVP supports the use of basemap replacement. The configuration file is in web_src/static/js/config.js. Please modify and recompile the front-end file.

```javascript
window.mapParam = {
  // Turn on/off map function
  enable: true,
  // coordinate system GCJ-02 WGS-84,
  coordinateSystem: "GCJ-02",
  // Map tile address
  tilesUrl: "http://webrd0{1-4}.is.autonavi.com/appmaptile?x={x}&y={y}&z={z}&lang=zh_cn&size=1&scale=1&style=8",
  // Tile size
  tileSize: 256,
  // Default level
  zoom:10,
  // Default map center point
  center:[116.41020, 39.915119],
  // The maximum level of the map
  maxZoom:18,
  // Minimum map level
  minZoom: 3
}
```
