
window.baseUrl = "http://10.10.1.124:18978"

// mapComponent global parameters, commenting this content can turn off the map function
window.mapParam = {
  // turn on/Turn off map function
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
