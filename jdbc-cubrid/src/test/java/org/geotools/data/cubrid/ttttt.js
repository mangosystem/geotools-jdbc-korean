Tmap.Layer.EventPane = Tmap
		.Class(
				Tmap.Layer,
				{
					smoothDragPan : true,
					isBaseLayer : true,
					isFixed : true,
					pane : null,
					mapObject : null,
					initialize : function(name, options) {
						Tmap.Layer.prototype.initialize.apply(this, arguments);
						if (this.pane == null) {
							this.pane = Tmap.Util.createDiv(this.div.id
									+ "_EventPane")
						}
					},
					destroy : function() {
						this.mapObject = null;
						this.pane = null;
						Tmap.Layer.prototype.destroy.apply(this, arguments)
					},
					setMap : function(map) {
						Tmap.Layer.prototype.setMap.apply(this, arguments);
						this.pane.style.zIndex = parseInt(this.div.style.zIndex) + 1;
						this.pane.style.display = this.div.style.display;
						this.pane.style.width = "100%";
						this.pane.style.height = "100%";
						if (Tmap.BROWSER_NAME == "msie") {
							this.pane.style.background = "url("
									+ Tmap.Util.getImageLocation("blank.gif")
									+ ")"
						}
						if (this.isFixed) {
							this.map.viewPortDiv.appendChild(this.pane)
						} else {
							this.map.layerContainerDiv.appendChild(this.pane)
						}
						this.loadMapObject();
						if (this.mapObject == null) {
							this.loadWarningMessage()
						}
					},
					removeMap : function(map) {
						if (this.pane && this.pane.parentNode) {
							this.pane.parentNode.removeChild(this.pane)
						}
						Tmap.Layer.prototype.removeMap.apply(this, arguments)
					},
					loadWarningMessage : function() {
						this.div.style.backgroundColor = "darkblue";
						var viewSize = this.map.getSize();
						var msgW = Math.min(viewSize.w, 300);
						var msgH = Math.min(viewSize.h, 200);
						var size = new Tmap.Size(msgW, msgH);
						var centerPx = new Tmap.Pixel(viewSize.w / 2,
								viewSize.h / 2);
						var topLeft = centerPx.add(-size.w / 2, -size.h / 2);
						var div = Tmap.Util.createDiv(this.name + "_warning",
								topLeft, size, null, null, null, "auto");
						div.style.padding = "7px";
						div.style.backgroundColor = "yellow";
						div.innerHTML = this.getWarningHTML();
						this.div.appendChild(div)
					},
					getWarningHTML : function() {
						return ""
					},
					display : function(display) {
						Tmap.Layer.prototype.display.apply(this, arguments);
						this.pane.style.display = this.div.style.display
					},
					setZIndex : function(zIndex) {
						Tmap.Layer.prototype.setZIndex.apply(this, arguments);
						this.pane.style.zIndex = parseInt(this.div.style.zIndex) + 1
					},
					moveByPx : function(dx, dy) {
						Tmap.Layer.prototype.moveByPx.apply(this, arguments);
						if (this.dragPanMapObject) {
							this.dragPanMapObject(dx, -dy)
						} else {
							this.moveTo(this.map.getCachedCenter())
						}
					},
					moveTo : function(bounds, zoomChanged, dragging) {
						Tmap.Layer.prototype.moveTo.apply(this, arguments);
						if (this.mapObject != null) {
							var newCenter = this.map.getCenter();
							var newZoom = this.map.getZoom();
							if (newCenter != null) {
								var moOldCenter = this.getMapObjectCenter();
								var oldCenter = this
										.getOLLonLatFromMapObjectLonLat(moOldCenter);
								var moOldZoom = this.getMapObjectZoom();
								var oldZoom = this
										.getOLZoomFromMapObjectZoom(moOldZoom);
								if (!(newCenter.equals(oldCenter))
										|| newZoom != oldZoom) {
									if (!zoomChanged && oldCenter
											&& this.dragPanMapObject
											&& this.smoothDragPan) {
										var oldPx = this.map
												.getViewPortPxFromLonLat(oldCenter);
										var newPx = this.map
												.getViewPortPxFromLonLat(newCenter);
										this.dragPanMapObject(
												newPx.x - oldPx.x, oldPx.y
														- newPx.y)
									} else {
										var center = this
												.getMapObjectLonLatFromTMLonLat(newCenter);
										var zoom = this
												.getMapObjectZoomFromTMZoom(newZoom);
										this.setMapObjectCenter(center, zoom,
												dragging)
									}
								}
							}
						}
					},
					getLonLatFromViewPortPx : function(viewPortPx) {
						var lonlat = null;
						if ((this.mapObject != null)
								&& (this.getMapObjectCenter() != null)) {
							var moPixel = this
									.getMapObjectPixelFromTMPixel(viewPortPx);
							var moLonLat = this
									.getMapObjectLonLatFromMapObjectPixel(moPixel);
							lonlat = this
									.getOLLonLatFromMapObjectLonLat(moLonLat)
						}
						return lonlat
					},
					getViewPortPxFromLonLat : function(lonlat) {
						var viewPortPx = null;
						if ((this.mapObject != null)
								&& (this.getMapObjectCenter() != null)) {
							var moLonLat = this
									.getMapObjectLonLatFromOLLonLat(lonlat);
							var moPixel = this
									.getMapObjectPixelFromMapObjectLonLat(moLonLat);
							viewPortPx = this
									.getOLPixelFromMapObjectPixel(moPixel)
						}
						return viewPortPx
					},
					getTMLonLatFromMapObjectLonLat : function(moLonLat) {
						var tmLonLat = null;
						if (moLonLat != null) {
							var lon = this
									.getLongitudeFromMapObjectLonLat(moLonLat);
							var lat = this
									.getLatitudeFromMapObjectLonLat(moLonLat);
							tmLonLat = new Tmap.LonLat(lon, lat)
						}
						return tmLonLat
					},
					getMapObjectLonLatFromTMLonLat : function(tmLonLat) {
						var moLatLng = null;
						if (tmLonLat != null) {
							moLatLng = this.getMapObjectLonLatFromLonLat(
									tmLonLat.lon, tmLonLat.lat)
						}
						return moLatLng
					},
					getTMPixelFromMapObjectPixel : function(moPixel) {
						var tmPixel = null;
						if (moPixel != null) {
							var x = this.getXFromMapObjectPixel(moPixel);
							var y = this.getYFromMapObjectPixel(moPixel);
							tmPixel = new Tmap.Pixel(x, y)
						}
						return tmPixel
					},
					getMapObjectPixelFromTMPixel : function(tmPixel) {
						var moPixel = null;
						if (tmPixel != null) {
							moPixel = this.getMapObjectPixelFromXY(tmPixel.x,
									tmPixel.y)
						}
						return moPixel
					},
					CLASS_NAME : "Tmap.Layer.EventPane"
				});
Tmap.Layer.FixedZoomLevels = Tmap
		.Class({
			initialize : function() {
			},
			initResolutions : function() {
				var props = [ 'minZoomLevel', 'maxZoomLevel', 'numZoomLevels' ];
				for ( var i = 0, len = props.length; i < len; i++) {
					var property = props[i];
					this[property] = (this.options[property] != null) ? this.options[property]
							: this.map[property]
				}
				if ((this.minZoomLevel == null)
						|| (this.minZoomLevel < this.MIN_ZOOM_LEVEL)) {
					this.minZoomLevel = this.MIN_ZOOM_LEVEL
				}
				var desiredZoomLevels;
				var limitZoomLevels = this.MAX_ZOOM_LEVEL - this.minZoomLevel
						+ 1;
				if (((this.options.numZoomLevels == null) && (this.options.maxZoomLevel != null))
						|| ((this.numZoomLevels == null) && (this.maxZoomLevel != null))) {
					desiredZoomLevels = this.maxZoomLevel - this.minZoomLevel
							+ 1
				} else {
					desiredZoomLevels = this.numZoomLevels
				}
				if (desiredZoomLevels != null) {
					this.numZoomLevels = Math.min(desiredZoomLevels,
							limitZoomLevels)
				} else {
					this.numZoomLevels = limitZoomLevels
				}
				this.maxZoomLevel = this.minZoomLevel + this.numZoomLevels - 1;
				if (this.RESOLUTIONS != null) {
					var resolutionsIndex = 0;
					this.resolutions = [];
					for ( var i = this.minZoomLevel; i <= this.maxZoomLevel; i++) {
						this.resolutions[resolutionsIndex++] = this.RESOLUTIONS[i]
					}
					this.maxResolution = this.resolutions[0];
					this.minResolution = this.resolutions[this.resolutions.length - 1]
				}
			},
			getResolution : function() {
				if (this.resolutions != null) {
					return Tmap.Layer.prototype.getResolution.apply(this,
							arguments)
				} else {
					var resolution = null;
					var viewSize = this.map.getSize();
					var extent = this.getExtent();
					if ((viewSize != null) && (extent != null)) {
						resolution = Math.max(extent.getWidth() / viewSize.w,
								extent.getHeight() / viewSize.h)
					}
					return resolution
				}
			},
			getExtent : function() {
				var size = this.map.getSize();
				var tl = this.getLonLatFromViewPortPx({
					x : 0,
					y : 0
				});
				var br = this.getLonLatFromViewPortPx({
					x : size.w,
					y : size.h
				});
				if ((tl != null) && (br != null)) {
					return new Tmap.Bounds(tl.lon, br.lat, br.lon, tl.lat)
				} else {
					return null
				}
			},
			getZoomForResolution : function(resolution) {
				if (this.resolutions != null) {
					return Tmap.Layer.prototype.getZoomForResolution.apply(
							this, arguments)
				} else {
					var extent = Tmap.Layer.prototype.getExtent.apply(this, []);
					return this.getZoomForExtent(extent)
				}
			},
			getTMZoomFromMapObjectZoom : function(moZoom) {
				var zoom = null;
				if (moZoom != null) {
					zoom = moZoom - this.minZoomLevel;
					if (this.map.baseLayer !== this) {
						zoom = this.map.baseLayer.getZoomForResolution(this
								.getResolutionForZoom(zoom))
					}
				}
				return zoom
			},
			getMapObjectZoomFromTMZoom : function(tmZoom) {
				var zoom = null;
				if (tmZoom != null) {
					zoom = tmZoom + this.minZoomLevel;
					if (this.map.baseLayer !== this) {
						zoom = this.getZoomForResolution(this.map.baseLayer
								.getResolutionForZoom(zoom))
					}
				}
				return zoom
			},
			CLASS_NAME : "Tmap.Layer.FixedZoomLevels"
		});
Tmap.Layer.HTTPRequest = Tmap
		.Class(
				Tmap.Layer,
				{
					URL_HASH_FACTOR : (Math.sqrt(5) - 1) / 2,
					url : null,
					params : null,
					reproject : false,
					initialize : function(name, url, params, options) {
						Tmap.Layer.prototype.initialize.apply(this, [ name,
								options ]);
						this.url = url;
						if (!this.params) {
							this.params = Tmap.Util.extend({}, params)
						}
					},
					destroy : function() {
						this.url = null;
						this.params = null;
						Tmap.Layer.prototype.destroy.apply(this, arguments)
					},
					clone : function(obj) {
						if (obj == null) {
							obj = new Tmap.Layer.HTTPRequest(this.name,
									this.url, this.params, this.getOptions())
						}
						obj = Tmap.Layer.prototype.clone.apply(this, [ obj ]);
						return obj
					},
					setUrl : function(newUrl) {
						this.url = newUrl
					},
					mergeNewParams : function(newParams) {
						this.params = Tmap.Util.extend(this.params, newParams);
						var ret = this.redraw();
						if (this.map != null) {
							this.map.events.triggerEvent("changelayer", {
								layer : this,
								property : "params"
							})
						}
						return ret
					},
					redraw : function(force) {
						if (force) {
							return this.mergeNewParams({
								"_tmSalt" : Math.random()
							})
						} else {
							return Tmap.Layer.prototype.redraw.apply(this, [])
						}
					},
					selectUrl : function(paramString, urls) {
						var product = 1;
						for ( var i = 0, len = paramString.length; i < len; i++) {
							product *= paramString.charCodeAt(i)
									* this.URL_HASH_FACTOR;
							product -= Math.floor(product)
						}
						return urls[Math.floor(product * urls.length)]
					},
					getFullRequestString : function(newParams, altUrl) {
						var url = altUrl || this.url;
						var allParams = Tmap.Util.extend({}, this.params);
						allParams = Tmap.Util.extend(allParams, newParams);
						var paramsString = Tmap.Util
								.getParameterString(allParams);
						if (Tmap.Util.isArray(url)) {
							url = this.selectUrl(paramsString, url)
						}
						var urlParams = Tmap.Util.upperCaseObject(Tmap.Util
								.getParameters(url));
						for ( var key in allParams) {
							if (key.toUpperCase() in urlParams) {
								delete allParams[key]
							}
						}
						paramsString = Tmap.Util.getParameterString(allParams);
						return Tmap.Util.urlAppend(url, paramsString)
					},
					CLASS_NAME : "Tmap.Layer.HTTPRequest"
				});
Tmap.Layer.Grid = Tmap
		.Class(
				Tmap.Layer.HTTPRequest,
				{
					tileSize : null,
					tileOriginCorner : "bl",
					tileOrigin : null,
					tileOptions : null,
					tileClass : Tmap.Tile.Image,
					grid : null,
					singleTile : false,
					ratio : 1.5,
					buffer : 0,
					transitionEffect : null,
					numLoadingTiles : 0,
					serverResolutions : null,
					loading : false,
					backBuffer : null,
					gridResolution : null,
					backBufferResolution : null,
					backBufferLonLat : null,
					backBufferTimerId : null,
					removeBackBufferDelay : null,
					className : null,
					gridLayout : null,
					rowSign : null,
					transitionendEvents : [ 'transitionend',
							'webkitTransitionEnd', 'otransitionend',
							'oTransitionEnd' ],
					initialize : function(name, url, params, options) {
						Tmap.Layer.HTTPRequest.prototype.initialize.apply(this,
								arguments);
						this.grid = [];
						this._removeBackBuffer = Tmap.Function.bind(
								this.removeBackBuffer, this);
						this.initProperties();
						this.rowSign = this.tileOriginCorner.substr(0, 1) === "t" ? 1
								: -1
					},
					initProperties : function() {
						if (this.options.removeBackBufferDelay === undefined) {
							this.removeBackBufferDelay = this.singleTile ? 0
									: 2500
						}
						if (this.options.className === undefined) {
							this.className = this.singleTile ? 'tmLayerGridSingleTile'
									: 'tmLayerGrid'
						}
					},
					setMap : function(map) {
						Tmap.Layer.HTTPRequest.prototype.setMap.call(this, map);
						Tmap.Element.addClass(this.div, this.className)
					},
					removeMap : function(map) {
						this.removeBackBuffer()
					},
					destroy : function() {
						this.removeBackBuffer();
						this.clearGrid();
						this.grid = null;
						this.tileSize = null;
						Tmap.Layer.HTTPRequest.prototype.destroy.apply(this,
								arguments)
					},
					clearGrid : function() {
						if (this.grid) {
							for ( var iRow = 0, len = this.grid.length; iRow < len; iRow++) {
								var row = this.grid[iRow];
								for ( var iCol = 0, clen = row.length; iCol < clen; iCol++) {
									var tile = row[iCol];
									this.destroyTile(tile)
								}
							}
							this.grid = [];
							this.gridResolution = null;
							this.gridLayout = null
						}
					},
					addOptions : function(newOptions, reinitialize) {
						var singleTileChanged = newOptions.singleTile !== undefined
								&& newOptions.singleTile !== this.singleTile;
						Tmap.Layer.HTTPRequest.prototype.addOptions.apply(this,
								arguments);
						if (this.map && singleTileChanged) {
							this.initProperties();
							this.clearGrid();
							this.tileSize = this.options.tileSize;
							this.setTileSize();
							this.moveTo(null, true)
						}
					},
					clone : function(obj) {
						if (obj == null) {
							obj = new Tmap.Layer.Grid(this.name, this.url,
									this.params, this.getOptions())
						}
						obj = Tmap.Layer.HTTPRequest.prototype.clone.apply(
								this, [ obj ]);
						if (this.tileSize != null) {
							obj.tileSize = this.tileSize.clone()
						}
						obj.grid = [];
						obj.gridResolution = null;
						obj.backBuffer = null;
						obj.backBufferTimerId = null;
						obj.loading = false;
						obj.numLoadingTiles = 0;
						return obj
					},
					moveTo : function(bounds, zoomChanged, dragging) {
						Tmap.Layer.HTTPRequest.prototype.moveTo.apply(this,
								arguments);
						bounds = bounds || this.map.getExtent();
						if (bounds != null) {
							var forceReTile = !this.grid.length || zoomChanged;
							var tilesBounds = this.getTilesBounds();
							var resolution = this.map.getResolution();
							var serverResolution = this
									.getServerResolution(resolution);
							if (this.singleTile) {
								if (forceReTile
										|| (!dragging && !tilesBounds
												.containsBounds(bounds))) {
									if (zoomChanged
											&& this.transitionEffect !== 'resize') {
										this.removeBackBuffer()
									}
									if (!zoomChanged
											|| this.transitionEffect === 'resize') {
										this.applyBackBuffer(resolution)
									}
									this.initSingleTile(bounds)
								}
							} else {
								forceReTile = forceReTile
										|| !tilesBounds
												.intersectsBounds(
														bounds,
														{
															worldBounds : this.map.baseLayer.wrapDateLine
																	&& this.map
																			.getMaxExtent()
														});
								if (forceReTile) {
									if (zoomChanged
											&& (this.transitionEffect === 'resize' || this.gridResolution === resolution)) {
										this.applyBackBuffer(resolution)
									}
									this.initGriddedTiles(bounds)
								} else {
									this.moveGriddedTiles()
								}
							}
						}
					},
					getTileData : function(loc) {
						var data = null, x = loc.lon, y = loc.lat, numRows = this.grid.length;
						if (this.map && numRows) {
							var res = this.map.getResolution(), tileWidth = this.tileSize.w, tileHeight = this.tileSize.h, bounds = this.grid[0][0].bounds, left = bounds.left, top = bounds.top;
							if (x < left) {
								if (this.map.baseLayer.wrapDateLine) {
									var worldWidth = this.map.getMaxExtent()
											.getWidth();
									var worldsAway = Math.ceil((left - x)
											/ worldWidth);
									x += worldWidth * worldsAway
								}
							}
							var dtx = (x - left) / (res * tileWidth);
							var dty = (top - y) / (res * tileHeight);
							var col = Math.floor(dtx);
							var row = Math.floor(dty);
							if (row >= 0 && row < numRows) {
								var tile = this.grid[row][col];
								if (tile) {
									data = {
										tile : tile,
										i : Math.floor((dtx - col) * tileWidth),
										j : Math
												.floor((dty - row) * tileHeight)
									}
								}
							}
						}
						return data
					},
					destroyTile : function(tile) {
						this.removeTileMonitoringHooks(tile);
						tile.destroy()
					},
					getServerResolution : function(resolution) {
						var distance = Number.POSITIVE_INFINITY;
						resolution = resolution || this.map.getResolution();
						if (this.serverResolutions
								&& Tmap.Util.indexOf(this.serverResolutions,
										resolution) === -1) {
							var i, newDistance, newResolution, serverResolution;
							for (i = this.serverResolutions.length - 1; i >= 0; i--) {
								newResolution = this.serverResolutions[i];
								newDistance = Math.abs(newResolution
										- resolution);
								if (newDistance > distance) {
									break
								}
								distance = newDistance;
								serverResolution = newResolution
							}
							resolution = serverResolution
						}
						return resolution
					},
					getServerZoom : function() {
						var resolution = this.getServerResolution();
						return this.serverResolutions ? Tmap.Util.indexOf(
								this.serverResolutions, resolution) : this.map
								.getZoomForResolution(resolution)
								+ (this.zoomOffset || 0)
					},
					applyBackBuffer : function(resolution) {
						if (this.backBufferTimerId !== null) {
							this.removeBackBuffer()
						}
						var backBuffer = this.backBuffer;
						if (!backBuffer) {
							backBuffer = this.createBackBuffer();
							if (!backBuffer) {
								return
							}
							if (resolution === this.gridResolution) {
								this.div.insertBefore(backBuffer,
										this.div.firstChild)
							} else {
								this.map.baseLayer.div.parentNode.insertBefore(
										backBuffer, this.map.baseLayer.div)
							}
							this.backBuffer = backBuffer;
							var topLeftTileBounds = this.grid[0][0].bounds;
							this.backBufferLonLat = {
								lon : topLeftTileBounds.left,
								lat : topLeftTileBounds.top
							};
							this.backBufferResolution = this.gridResolution
						}
						var ratio = this.backBufferResolution / resolution;
						var tiles = backBuffer.childNodes, tile;
						for ( var i = tiles.length - 1; i >= 0; --i) {
							tile = tiles[i];
							tile.style.top = ((ratio * tile._i * tile._h) | 0)
									+ 'px';
							tile.style.left = ((ratio * tile._j * tile._w) | 0)
									+ 'px';
							tile.style.width = Math.round(ratio * tile._w)
									+ 'px';
							tile.style.height = Math.round(ratio * tile._h)
									+ 'px'
						}
						var position = this.getViewPortPxFromLonLat(
								this.backBufferLonLat, resolution);
						var leftOffset = this.map.layerContainerOriginPx.x;
						var topOffset = this.map.layerContainerOriginPx.y;
						backBuffer.style.left = Math.round(position.x
								- leftOffset)
								+ 'px';
						backBuffer.style.top = Math.round(position.y
								- topOffset)
								+ 'px'
					},
					createBackBuffer : function() {
						var backBuffer;
						if (this.grid.length > 0) {
							backBuffer = document.createElement('div');
							backBuffer.id = this.div.id + '_bb';
							backBuffer.className = 'tmBackBuffer';
							backBuffer.style.position = 'absolute';
							for ( var i = 0, lenI = this.grid.length; i < lenI; i++) {
								for ( var j = 0, lenJ = this.grid[i].length; j < lenJ; j++) {
									var tile = this.grid[i][j], markup = this.grid[i][j]
											.createBackBuffer();
									if (markup) {
										markup._i = i;
										markup._j = j;
										markup._w = tile.size.w;
										markup._h = tile.size.h;
										markup.id = tile.id + '_bb';
										backBuffer.appendChild(markup)
									}
								}
							}
						}
						return backBuffer
					},
					removeBackBuffer : function() {
						if (this._transitionElement) {
							for ( var i = this.transitionendEvents.length - 1; i >= 0; --i) {
								Tmap.Event.stopObserving(
										this._transitionElement,
										this.transitionendEvents[i],
										this._removeBackBuffer)
							}
							delete this._transitionElement
						}
						if (this.backBuffer) {
							if (this.backBuffer.parentNode) {
								this.backBuffer.parentNode
										.removeChild(this.backBuffer)
							}
							this.backBuffer = null;
							this.backBufferResolution = null;
							if (this.backBufferTimerId !== null) {
								window.clearTimeout(this.backBufferTimerId);
								this.backBufferTimerId = null
							}
						}
					},
					moveByPx : function(dx, dy) {
						if (!this.singleTile) {
							this.moveGriddedTiles()
						}
					},
					setTileSize : function(size) {
						if (this.singleTile) {
							size = this.map.getSize();
							size.h = parseInt(size.h * this.ratio, 10);
							size.w = parseInt(size.w * this.ratio, 10)
						}
						Tmap.Layer.HTTPRequest.prototype.setTileSize.apply(
								this, [ size ])
					},
					getTilesBounds : function() {
						var bounds = null;
						var length = this.grid.length;
						if (length) {
							var bottomLeftTileBounds = this.grid[length - 1][0].bounds, width = this.grid[0].length
									* bottomLeftTileBounds.getWidth(), height = this.grid.length
									* bottomLeftTileBounds.getHeight();
							bounds = new Tmap.Bounds(bottomLeftTileBounds.left,
									bottomLeftTileBounds.bottom,
									bottomLeftTileBounds.left + width,
									bottomLeftTileBounds.bottom + height)
						}
						return bounds
					},
					initSingleTile : function(bounds) {
						this.events.triggerEvent("retile");
						var center = bounds.getCenterLonLat();
						var tileWidth = bounds.getWidth() * this.ratio;
						var tileHeight = bounds.getHeight() * this.ratio;
						var tileBounds = new Tmap.Bounds(center.lon
								- (tileWidth / 2), center.lat
								- (tileHeight / 2), center.lon
								+ (tileWidth / 2), center.lat
								+ (tileHeight / 2));
						var px = this.map.getLayerPxFromLonLat({
							lon : tileBounds.left,
							lat : tileBounds.top
						});
						if (!this.grid.length) {
							this.grid[0] = []
						}
						var tile = this.grid[0][0];
						if (!tile) {
							tile = this.addTile(tileBounds, px);
							this.addTileMonitoringHooks(tile);
							tile.draw();
							this.grid[0][0] = tile
						} else {
							tile.moveTo(tileBounds, px)
						}
						this.removeExcessTiles(1, 1);
						this.gridResolution = this.getServerResolution()
					},
					calculateGridLayout : function(bounds, origin, resolution) {
						var tilelon = resolution * this.tileSize.w;
						var tilelat = resolution * this.tileSize.h;
						var offsetlon = bounds.left - origin.lon;
						var tilecol = Math.floor(offsetlon / tilelon)
								- this.buffer;
						var rowSign = this.rowSign;
						var offsetlat = rowSign
								* (origin.lat - bounds.top + tilelat);
						var tilerow = Math[~rowSign ? 'floor' : 'ceil']
								(offsetlat / tilelat)
								- this.buffer * rowSign;
						return {
							tilelon : tilelon,
							tilelat : tilelat,
							startcol : tilecol,
							startrow : tilerow
						}
					},
					getTileOrigin : function() {
						var origin = this.tileOrigin;
						if (!origin) {
							var extent = this.getMaxExtent();
							var edges = ({
								"tl" : [ "left", "top" ],
								"tr" : [ "right", "top" ],
								"bl" : [ "left", "bottom" ],
								"br" : [ "right", "bottom" ]
							})[this.tileOriginCorner];
							origin = new Tmap.LonLat(extent[edges[0]],
									extent[edges[1]])
						}
						return origin
					},
					getTileBoundsForGridIndex : function(row, col) {
						var origin = this.getTileOrigin();
						var tileLayout = this.gridLayout;
						var tilelon = tileLayout.tilelon;
						var tilelat = tileLayout.tilelat;
						var startcol = tileLayout.startcol;
						var startrow = tileLayout.startrow;
						var rowSign = this.rowSign;
						return new Tmap.Bounds(origin.lon + (startcol + col)
								* tilelon, origin.lat
								- (startrow + row * rowSign) * tilelat
								* rowSign, origin.lon + (startcol + col + 1)
								* tilelon, origin.lat
								- (startrow + (row - 1) * rowSign) * tilelat
								* rowSign)
					},
					initGriddedTiles : function(bounds) {
						this.events.triggerEvent("retile");
						var viewSize = this.map.getSize();
						var origin = this.getTileOrigin();
						var resolution = this.map.getResolution(), serverResolution = this
								.getServerResolution(), ratio = resolution
								/ serverResolution, tileSize = {
							w : this.tileSize.w / ratio,
							h : this.tileSize.h / ratio
						};
						var minRows = Math.ceil(viewSize.h / tileSize.h) + 2
								* this.buffer + 1;
						var minCols = Math.ceil(viewSize.w / tileSize.w) + 2
								* this.buffer + 1;
						var tileLayout = this.calculateGridLayout(bounds,
								origin, serverResolution);
						this.gridLayout = tileLayout;
						var tilelon = tileLayout.tilelon;
						var tilelat = tileLayout.tilelat;
						var layerContainerDivLeft = this.map.layerContainerOriginPx.x;
						var layerContainerDivTop = this.map.layerContainerOriginPx.y;
						var tileBounds = this.getTileBoundsForGridIndex(0, 0);
						var startPx = this.map
								.getViewPortPxFromLonLat(new Tmap.LonLat(
										tileBounds.left, tileBounds.top));
						startPx.x = Math.round(startPx.x)
								- layerContainerDivLeft;
						startPx.y = Math.round(startPx.y)
								- layerContainerDivTop;
						var tileData = [], center = this.map.getCenter();
						var rowidx = 0;
						do {
							var row = this.grid[rowidx];
							if (!row) {
								row = [];
								this.grid.push(row)
							}
							var colidx = 0;
							do {
								tileBounds = this.getTileBoundsForGridIndex(
										rowidx, colidx);
								var px = startPx.clone();
								px.x = px.x + colidx * Math.round(tileSize.w);
								px.y = px.y + rowidx * Math.round(tileSize.h);
								var tile = row[colidx];
								if (!tile) {
									tile = this.addTile(tileBounds, px);
									this.addTileMonitoringHooks(tile);
									row.push(tile)
								} else {
									tile.moveTo(tileBounds, px, false)
								}
								var tileCenter = tileBounds.getCenterLonLat();
								tileData.push({
									tile : tile,
									distance : Math.pow(tileCenter.lon
											- center.lon, 2)
											+ Math.pow(tileCenter.lat
													- center.lat, 2)
								});
								colidx += 1
							} while ((tileBounds.right <= bounds.right
									+ tilelon * this.buffer)
									|| colidx < minCols);
							rowidx += 1
						} while ((tileBounds.bottom >= bounds.bottom - tilelat
								* this.buffer)
								|| rowidx < minRows);
						this.removeExcessTiles(rowidx, colidx);
						var resolution = this.getServerResolution();
						this.gridResolution = resolution;
						tileData.sort(function(a, b) {
							return a.distance - b.distance
						});
						for ( var i = 0, ii = tileData.length; i < ii; ++i) {
							tileData[i].tile.draw()
						}
					},
					getMaxExtent : function() {
						return this.maxExtent
					},
					addTile : function(bounds, position) {
						var tile = new this.tileClass(this, position, bounds,
								null, this.tileSize, this.tileOptions);
						this.events.triggerEvent("addtile", {
							tile : tile
						});
						return tile
					},
					addTileMonitoringHooks : function(tile) {
						var replacingCls = 'olTileReplacing';
						tile.onLoadStart = function() {
							if (this.loading === false) {
								this.loading = true;
								this.events.triggerEvent("loadstart")
							}
							this.events.triggerEvent("tileloadstart", {
								tile : tile
							});
							this.numLoadingTiles++;
							if (!this.singleTile
									&& this.backBuffer
									&& this.gridResolution === this.backBufferResolution) {
								Tmap.Element
										.addClass(tile.imgDiv, replacingCls)
							}
						};
						tile.onLoadEnd = function(evt) {
							this.numLoadingTiles--;
							var aborted = evt.type === 'unload';
							this.events.triggerEvent("tileloaded", {
								tile : tile,
								aborted : aborted
							});
							if (!this.singleTile
									&& !aborted
									&& this.backBuffer
									&& this.gridResolution === this.backBufferResolution) {
								if (Tmap.Element.getStyle(tile.imgDiv,
										'display') === 'none') {
									var bufferTile = document
											.getElementById(tile.id + '_bb');
									if (bufferTile) {
										bufferTile.parentNode
												.removeChild(bufferTile)
									}
								}
								Tmap.Element.removeClass(tile.imgDiv,
										replacingCls)
							}
							if (this.numLoadingTiles === 0) {
								if (this.backBuffer) {
									this._transitionElement = tile.imgDiv;
									for ( var i = this.transitionendEvents.length - 1; i >= 0; --i) {
										Tmap.Event.observe(
												this._transitionElement,
												this.transitionendEvents[i],
												this._removeBackBuffer)
									}
									this.backBufferTimerId = window.setTimeout(
											this._removeBackBuffer,
											this.removeBackBufferDelay)
								}
								this.loading = false;
								this.events.triggerEvent("loadend")
							}
						};
						tile.onLoadError = function() {
							this.events.triggerEvent("tileerror", {
								tile : tile
							})
						};
						tile.events.on({
							"loadstart" : tile.onLoadStart,
							"loadend" : tile.onLoadEnd,
							"unload" : tile.onLoadEnd,
							"loaderror" : tile.onLoadError,
							scope : this
						})
					},
					removeTileMonitoringHooks : function(tile) {
						tile.unload();
						tile.events.un({
							"loadstart" : tile.onLoadStart,
							"loadend" : tile.onLoadEnd,
							"unload" : tile.onLoadEnd,
							"loaderror" : tile.onLoadError,
							scope : this
						})
					},
					moveGriddedTiles : function() {
						var buffer = this.buffer + 1;
						while (true) {
							var tlTile = this.grid[0][0];
							var tlViewPort = {
								x : tlTile.position.x
										+ this.map.layerContainerOriginPx.x,
								y : tlTile.position.y
										+ this.map.layerContainerOriginPx.y
							};
							var ratio = this.getServerResolution()
									/ this.map.getResolution();
							var tileSize = {
								w : Math.round(this.tileSize.w * ratio),
								h : Math.round(this.tileSize.h * ratio)
							};
							if (tlViewPort.x > -tileSize.w * (buffer - 1)) {
								this.shiftColumn(true, tileSize)
							} else if (tlViewPort.x < -tileSize.w * buffer) {
								this.shiftColumn(false, tileSize)
							} else if (tlViewPort.y > -tileSize.h
									* (buffer - 1)) {
								this.shiftRow(true, tileSize)
							} else if (tlViewPort.y < -tileSize.h * buffer) {
								this.shiftRow(false, tileSize)
							} else {
								break
							}
						}
					},
					shiftRow : function(prepend, tileSize) {
						var grid = this.grid;
						var rowIndex = prepend ? 0 : (grid.length - 1);
						var sign = prepend ? -1 : 1;
						var rowSign = this.rowSign;
						var tileLayout = this.gridLayout;
						tileLayout.startrow += sign * rowSign;
						var modelRow = grid[rowIndex];
						var row = grid[prepend ? 'pop' : 'shift']();
						for ( var i = 0, len = row.length; i < len; i++) {
							var tile = row[i];
							var position = modelRow[i].position.clone();
							position.y += tileSize.h * sign;
							tile.moveTo(this.getTileBoundsForGridIndex(
									rowIndex, i), position)
						}
						grid[prepend ? 'unshift' : 'push'](row)
					},
					shiftColumn : function(prepend, tileSize) {
						var grid = this.grid;
						var colIndex = prepend ? 0 : (grid[0].length - 1);
						var sign = prepend ? -1 : 1;
						var tileLayout = this.gridLayout;
						tileLayout.startcol += sign;
						for ( var i = 0, len = grid.length; i < len; i++) {
							var row = grid[i];
							var position = row[colIndex].position.clone();
							var tile = row[prepend ? 'pop' : 'shift']();
							position.x += tileSize.w * sign;
							tile.moveTo(this.getTileBoundsForGridIndex(i,
									colIndex), position);
							row[prepend ? 'unshift' : 'push'](tile)
						}
					},
					removeExcessTiles : function(rows, columns) {
						var i, l;
						while (this.grid.length > rows) {
							var row = this.grid.pop();
							for (i = 0, l = row.length; i < l; i++) {
								var tile = row[i];
								this.destroyTile(tile)
							}
						}
						for (i = 0, l = this.grid.length; i < l; i++) {
							while (this.grid[i].length > columns) {
								var row = this.grid[i];
								var tile = row.pop();
								this.destroyTile(tile)
							}
						}
					},
					onMapResize : function() {
						if (this.singleTile) {
							this.clearGrid();
							this.setTileSize()
						}
					},
					getTileBounds : function(viewPortPx) {
						var maxExtent = this.maxExtent;
						var resolution = this.getResolution();
						var tileMapWidth = resolution * this.tileSize.w;
						var tileMapHeight = resolution * this.tileSize.h;
						var mapPoint = this.getLonLatFromViewPortPx(viewPortPx);
						var tileLeft = maxExtent.left
								+ (tileMapWidth * Math
										.floor((mapPoint.lon - maxExtent.left)
												/ tileMapWidth));
						var tileBottom = maxExtent.bottom
								+ (tileMapHeight * Math
										.floor((mapPoint.lat - maxExtent.bottom)
												/ tileMapHeight));
						return new Tmap.Bounds(tileLeft, tileBottom, tileLeft
								+ tileMapWidth, tileBottom + tileMapHeight)
					},
					CLASS_NAME : "Tmap.Layer.Grid"
				});
Tmap.Layer.MapServer = Tmap
		.Class(
				Tmap.Layer.Grid,
				{
					DEFAULT_PARAMS : {
						mode : "map",
						map_imagetype : "png"
					},
					initialize : function(name, url, params, options) {
						Tmap.Layer.Grid.prototype.initialize.apply(this,
								arguments);
						this.params = Tmap.Util.applyDefaults(this.params,
								this.DEFAULT_PARAMS);
						if (options == null || options.isBaseLayer == null) {
							this.isBaseLayer = ((this.params.transparent != "true") && (this.params.transparent != true))
						}
					},
					clone : function(obj) {
						if (obj == null) {
							obj = new Tmap.Layer.MapServer(this.name, this.url,
									this.params, this.getOptions())
						}
						obj = Tmap.Layer.Grid.prototype.clone.apply(this,
								[ obj ]);
						return obj
					},
					getURL : function(bounds) {
						bounds = this.adjustBounds(bounds);
						var extent = [ bounds.left, bounds.bottom,
								bounds.right, bounds.top ];
						var imageSize = this.getImageSize();
						var url = this.getFullRequestString({
							mapext : extent,
							imgext : extent,
							map_size : [ imageSize.w, imageSize.h ],
							imgx : imageSize.w / 2,
							imgy : imageSize.h / 2,
							imgxy : [ imageSize.w, imageSize.h ]
						});
						return url
					},
					getFullRequestString : function(newParams, altUrl) {
						var url = (altUrl == null) ? this.url : altUrl;
						var allParams = Tmap.Util.extend({}, this.params);
						allParams = Tmap.Util.extend(allParams, newParams);
						var paramsString = Tmap.Util
								.getParameterString(allParams);
						if (Tmap.Util.isArray(url)) {
							url = this.selectUrl(paramsString, url)
						}
						var urlParams = Tmap.Util.upperCaseObject(Tmap.Util
								.getParameters(url));
						for ( var key in allParams) {
							if (key.toUpperCase() in urlParams) {
								delete allParams[key]
							}
						}
						paramsString = Tmap.Util.getParameterString(allParams);
						var requestString = url;
						paramsString = paramsString.replace(/,/g, "+");
						if (paramsString != "") {
							var lastServerChar = url.charAt(url.length - 1);
							if ((lastServerChar == "&")
									|| (lastServerChar == "?")) {
								requestString += paramsString
							} else {
								if (url.indexOf('?') == -1) {
									requestString += '?' + paramsString
								} else {
									requestString += '&' + paramsString
								}
							}
						}
						return requestString
					},
					CLASS_NAME : "Tmap.Layer.MapServer"
				});
Tmap.Layer.Markers = Tmap.Class(Tmap.Layer, {
	isBaseLayer : false,
	markers : null,
	drawn : false,
	initialize : function(name, options) {
		Tmap.Layer.prototype.initialize.apply(this, arguments);
		this.markers = []
	},
	destroy : function() {
		this.clearMarkers();
		this.markers = null;
		Tmap.Layer.prototype.destroy.apply(this, arguments)
	},
	setOpacity : function(opacity) {
		if (opacity != this.opacity) {
			this.opacity = opacity;
			for ( var i = 0, len = this.markers.length; i < len; i++) {
				this.markers[i].setOpacity(this.opacity)
			}
		}
	},
	moveTo : function(bounds, zoomChanged, dragging) {
		Tmap.Layer.prototype.moveTo.apply(this, arguments);
		if (zoomChanged || !this.drawn) {
			for ( var i = 0, len = this.markers.length; i < len; i++) {
				this.drawMarker(this.markers[i])
			}
			this.drawn = true
		}
	},
	addMarker : function(marker) {
		this.markers.push(marker);
		if (this.opacity < 1) {
			marker.setOpacity(this.opacity)
		}
		if (this.map && this.map.getExtent()) {
			marker.map = this.map;
			this.drawMarker(marker)
		}
	},
	removeMarker : function(marker) {
		if (this.markers && this.markers.length) {
			Tmap.Util.removeItem(this.markers, marker);
			marker.erase()
		}
	},
	clearMarkers : function() {
		if (this.markers != null) {
			while (this.markers.length > 0) {
				this.removeMarker(this.markers[0])
			}
		}
	},
	drawMarker : function(marker) {
		var px = this.map.getLayerPxFromLonLat(marker.lonlat);
		if (px == null) {
			marker.display(false)
		} else {
			if (!marker.isDrawn()) {
				var markerImg = marker.draw(px);
				this.div.appendChild(markerImg)
			} else if (marker.icon) {
				marker.icon.moveTo(px)
			}
		}
	},
	getDataExtent : function() {
		var maxExtent = null;
		if (this.markers && (this.markers.length > 0)) {
			var maxExtent = new Tmap.Bounds();
			for ( var i = 0, len = this.markers.length; i < len; i++) {
				var marker = this.markers[i];
				maxExtent.extend(marker.lonlat)
			}
		}
		return maxExtent
	},
	CLASS_NAME : "Tmap.Layer.Markers"
});
Tmap.Layer.Text = Tmap
		.Class(
				Tmap.Layer.Markers,
				{
					location : null,
					features : null,
					formatOptions : null,
					selectedFeature : null,
					initialize : function(name, options) {
						Tmap.Layer.Markers.prototype.initialize.apply(this,
								arguments);
						this.features = []
					},
					destroy : function() {
						Tmap.Layer.Markers.prototype.destroy.apply(this,
								arguments);
						this.clearFeatures();
						this.features = null
					},
					loadText : function() {
						if (!this.loaded) {
							if (this.location != null) {
								var onFail = function(e) {
									this.events.triggerEvent("loadend")
								};
								this.events.triggerEvent("loadstart");
								Tmap.Request.GET({
									url : this.location,
									success : this.parseData,
									failure : onFail,
									scope : this
								});
								this.loaded = true
							}
						}
					},
					moveTo : function(bounds, zoomChanged, minor) {
						Tmap.Layer.Markers.prototype.moveTo.apply(this,
								arguments);
						if (this.visibility && !this.loaded) {
							this.loadText()
						}
					},
					parseData : function(ajaxRequest) {
						var text = ajaxRequest.responseText;
						var options = {};
						Tmap.Util.extend(options, this.formatOptions);
						if (this.map
								&& !this.projection.equals(this.map
										.getProjectionObject())) {
							options.externalProjection = this.projection;
							options.internalProjection = this.map
									.getProjectionObject()
						}
						var parser = new Tmap.Format.Text(options);
						var features = parser.read(text);
						for ( var i = 0, len = features.length; i < len; i++) {
							var data = {};
							var feature = features[i];
							var location;
							var iconSize, iconOffset;
							location = new Tmap.LonLat(feature.geometry.x,
									feature.geometry.y);
							if (feature.style.graphicWidth
									&& feature.style.graphicHeight) {
								iconSize = new Tmap.Size(
										feature.style.graphicWidth,
										feature.style.graphicHeight)
							}
							if (feature.style.graphicXOffset !== undefined
									&& feature.style.graphicYOffset !== undefined) {
								iconOffset = new Tmap.Pixel(
										feature.style.graphicXOffset,
										feature.style.graphicYOffset)
							}
							if (feature.style.externalGraphic != null) {
								data.icon = new Tmap.Icon(
										feature.style.externalGraphic,
										iconSize, iconOffset)
							} else {
								data.icon = Tmap.Marker.defaultIcon();
								if (iconSize != null) {
									data.icon.setSize(iconSize)
								}
							}
							if ((feature.attributes.title != null)
									&& (feature.attributes.description != null)) {
								data['popupContentHTML'] = '<h2>'
										+ feature.attributes.title + '</h2>'
										+ '<p>'
										+ feature.attributes.description
										+ '</p>'
							}
							data['overflow'] = feature.attributes.overflow
									|| "auto";
							var markerFeature = new Tmap.Feature(this,
									location, data);
							this.features.push(markerFeature);
							var marker = markerFeature.createMarker();
							if ((feature.attributes.title != null)
									&& (feature.attributes.description != null)) {
								marker.events.register('click', markerFeature,
										this.markerClick)
							}
							this.addMarker(marker)
						}
						this.events.triggerEvent("loadend")
					},
					markerClick : function(evt) {
						var sameMarkerClicked = (this == this.layer.selectedFeature);
						this.layer.selectedFeature = (!sameMarkerClicked) ? this
								: null;
						for ( var i = 0, len = this.layer.map.popups.length; i < len; i++) {
							this.layer.map
									.removePopup(this.layer.map.popups[i])
						}
						if (!sameMarkerClicked) {
							this.layer.map.addPopup(this.createPopup())
						}
						Tmap.Event.stop(evt)
					},
					clearFeatures : function() {
						if (this.features != null) {
							while (this.features.length > 0) {
								var feature = this.features[0];
								Tmap.Util.removeItem(this.features, feature);
								feature.destroy()
							}
						}
					},
					CLASS_NAME : "Tmap.Layer.Text"
				});
Tmap.Layer.WorldWind = Tmap.Class(Tmap.Layer.Grid, {
	DEFAULT_PARAMS : {},
	isBaseLayer : true,
	lzd : null,
	zoomLevels : null,
	initialize : function(name, url, lzd, zoomLevels, params, options) {
		this.lzd = lzd;
		this.zoomLevels = zoomLevels;
		var newArguments = [];
		newArguments.push(name, url, params, options);
		Tmap.Layer.Grid.prototype.initialize.apply(this, newArguments);
		this.params = Tmap.Util.applyDefaults(this.params, this.DEFAULT_PARAMS)
	},
	getZoom : function() {
		var zoom = this.map.getZoom();
		var extent = this.map.getMaxExtent();
		zoom = zoom - Math.log(this.maxResolution / (this.lzd / 512))
				/ Math.log(2);
		return zoom
	},
	getURL : function(bounds) {
		bounds = this.adjustBounds(bounds);
		var zoom = this.getZoom();
		var extent = this.map.getMaxExtent();
		var deg = this.lzd / Math.pow(2, this.getZoom());
		var x = Math.floor((bounds.left - extent.left) / deg);
		var y = Math.floor((bounds.bottom - extent.bottom) / deg);
		if (this.map.getResolution() <= (this.lzd / 512)
				&& this.getZoom() <= this.zoomLevels) {
			return this.getFullRequestString({
				L : zoom,
				X : x,
				Y : y
			})
		} else {
			return Tmap.Util.getImageLocation("blank.gif")
		}
	},
	CLASS_NAME : "Tmap.Layer.WorldWind"
});
Tmap.Layer.WMS = Tmap
		.Class(
				Tmap.Layer.Grid,
				{
					DEFAULT_PARAMS : {
						service : "WMS",
						version : "1.1.1",
						request : "GetMap",
						styles : "",
						format : "image/jpeg"
					},
					isBaseLayer : true,
					encodeBBOX : false,
					noMagic : false,
					yx : {},
					initialize : function(name, url, params, options) {
						var newArguments = [];
						params = Tmap.Util.upperCaseObject(params);
						if (parseFloat(params.VERSION) >= 1.3
								&& !params.EXCEPTIONS) {
							params.EXCEPTIONS = "INIMAGE"
						}
						newArguments.push(name, url, params, options);
						Tmap.Layer.Grid.prototype.initialize.apply(this,
								newArguments);
						Tmap.Util.applyDefaults(this.params, Tmap.Util
								.upperCaseObject(this.DEFAULT_PARAMS));
						if (!this.noMagic
								&& this.params.TRANSPARENT
								&& this.params.TRANSPARENT.toString()
										.toLowerCase() == "true") {
							if ((options == null) || (!options.isBaseLayer)) {
								this.isBaseLayer = false
							}
							if (this.params.FORMAT == "image/jpeg") {
								this.params.FORMAT = Tmap.Util.alphaHack() ? "image/gif"
										: "image/png"
							}
						}
					},
					clone : function(obj) {
						if (obj == null) {
							obj = new Tmap.Layer.WMS(this.name, this.url,
									this.params, this.getOptions())
						}
						obj = Tmap.Layer.Grid.prototype.clone.apply(this,
								[ obj ]);
						return obj
					},
					reverseAxisOrder : function() {
						var projCode = this.projection.getCode();
						return parseFloat(this.params.VERSION) >= 1.3
								&& !!(this.yx[projCode] || (Tmap.Projection.defaults[projCode] && Tmap.Projection.defaults[projCode].yx))
					},
					getURL : function(bounds) {
						bounds = this.adjustBounds(bounds);
						var imageSize = this.getImageSize();
						var newParams = {};
						var reverseAxisOrder = this.reverseAxisOrder();
						newParams.BBOX = this.encodeBBOX ? bounds.toBBOX(null,
								reverseAxisOrder) : bounds
								.toArray(reverseAxisOrder);
						newParams.WIDTH = imageSize.w;
						newParams.HEIGHT = imageSize.h;
						var requestString = this
								.getFullRequestString(newParams);
						return requestString
					},
					mergeNewParams : function(newParams) {
						var upperParams = Tmap.Util.upperCaseObject(newParams);
						var newArguments = [ upperParams ];
						return Tmap.Layer.Grid.prototype.mergeNewParams.apply(
								this, newArguments)
					},
					getFullRequestString : function(newParams, altUrl) {
						var mapProjection = this.map.getProjectionObject();
						var projectionCode = this.projection
								&& this.projection.equals(mapProjection) ? this.projection
								.getCode()
								: mapProjection.getCode();
						var value = (projectionCode == "none") ? null
								: projectionCode;
						if (parseFloat(this.params.VERSION) >= 1.3) {
							this.params.CRS = value
						} else {
							this.params.SRS = value
						}
						if (typeof this.params.TRANSPARENT == "boolean") {
							newParams.TRANSPARENT = this.params.TRANSPARENT ? "TRUE"
									: "FALSE"
						}
						return Tmap.Layer.Grid.prototype.getFullRequestString
								.apply(this, arguments)
					},
					CLASS_NAME : "Tmap.Layer.WMS"
				});
Tmap.Layer.Boxes = Tmap.Class(Tmap.Layer.Markers, {
	drawMarker : function(marker) {
		var topleft = this.map.getLayerPxFromLonLat({
			lon : marker.bounds.left,
			lat : marker.bounds.top
		});
		var botright = this.map.getLayerPxFromLonLat({
			lon : marker.bounds.right,
			lat : marker.bounds.bottom
		});
		if (botright == null || topleft == null) {
			marker.display(false)
		} else {
			var markerDiv = marker.draw(topleft, {
				w : Math.max(1, botright.x - topleft.x),
				h : Math.max(1, botright.y - topleft.y)
			});
			if (!marker.drawn) {
				this.div.appendChild(markerDiv);
				marker.drawn = true
			}
		}
	},
	removeMarker : function(marker) {
		Tmap.Util.removeItem(this.markers, marker);
		if ((marker.div != null) && (marker.div.parentNode == this.div)) {
			this.div.removeChild(marker.div)
		}
	},
	CLASS_NAME : "Tmap.Layer.Boxes"
});
Tmap.Layer.XYZ = Tmap.Class(Tmap.Layer.Grid, {
	isBaseLayer : true,
	sphericalMercator : false,
	zoomOffset : 0,
	serverResolutions : null,
	initialize : function(name, url, options) {
		if (options && options.sphericalMercator || this.sphericalMercator) {
			options = Tmap.Util.extend({
				projection : "EPSG:900913",
				numZoomLevels : 19
			}, options)
		}
		Tmap.Layer.Grid.prototype.initialize.apply(this, [ name || this.name,
				url || this.url, {}, options ])
	},
	clone : function(obj) {
		if (obj == null) {
			obj = new Tmap.Layer.XYZ(this.name, this.url, this.getOptions())
		}
		obj = Tmap.Layer.Grid.prototype.clone.apply(this, [ obj ]);
		return obj
	},
	getURL : function(bounds) {
		var xyz = this.getXYZ(bounds);
		var url = this.url;
		if (Tmap.Util.isArray(url)) {
			var s = '' + xyz.x + xyz.y + xyz.z;
			url = this.selectUrl(s, url)
		}
		return Tmap.String.format(url, xyz)
	},
	getXYZ : function(bounds) {
		var res = this.getServerResolution();
		var x = Math.round((bounds.left - this.maxExtent.left)
				/ (res * this.tileSize.w));
		var y = Math.round((this.maxExtent.top - bounds.top)
				/ (res * this.tileSize.h));
		var z = this.getServerZoom();
		if (this.wrapDateLine) {
			var limit = Math.pow(2, z);
			x = ((x % limit) + limit) % limit
		}
		return {
			'x' : x,
			'y' : y,
			'z' : z
		}
	},
	setMap : function(map) {
		Tmap.Layer.Grid.prototype.setMap.apply(this, arguments);
		if (!this.tileOrigin) {
			this.tileOrigin = new Tmap.LonLat(this.maxExtent.left,
					this.maxExtent.bottom)
		}
	},
	CLASS_NAME : "Tmap.Layer.XYZ"
});
Tmap.Layer.UTFGrid = Tmap.Class(Tmap.Layer.XYZ, {
	isBaseLayer : false,
	projection : new Tmap.Projection("EPSG:900913"),
	useJSONP : false,
	tileClass : Tmap.Tile.UTFGrid,
	initialize : function(options) {
		Tmap.Layer.Grid.prototype.initialize.apply(this, [ options.name,
				options.url, {}, options ]);
		this.tileOptions = Tmap.Util.extend({
			utfgridResolution : this.utfgridResolution
		}, this.tileOptions)
	},
	createBackBuffer : function() {
	},
	clone : function(obj) {
		if (obj == null) {
			obj = new Tmap.Layer.UTFGrid(this.getOptions())
		}
		obj = Tmap.Layer.Grid.prototype.clone.apply(this, [ obj ]);
		return obj
	},
	getFeatureInfo : function(location) {
		var info = null;
		var tileInfo = this.getTileData(location);
		if (tileInfo && tileInfo.tile) {
			info = tileInfo.tile.getFeatureInfo(tileInfo.i, tileInfo.j)
		}
		return info
	},
	getFeatureId : function(location) {
		var id = null;
		var info = this.getTileData(location);
		if (info.tile) {
			id = info.tile.getFeatureId(info.i, info.j)
		}
		return id
	},
	CLASS_NAME : "Tmap.Layer.UTFGrid"
});
Tmap.Layer.OSM = Tmap
		.Class(
				Tmap.Layer.XYZ,
				{
					name : "OpenStreetMap",
					url : [
							'http://a.tile.openstreetmap.org/${z}/${x}/${y}.png',
							'http://b.tile.openstreetmap.org/${z}/${x}/${y}.png',
							'http://c.tile.openstreetmap.org/${z}/${x}/${y}.png' ],
					attribution : "&copy; <a href='http://www.openstreetmap.org/copyright'>OpenStreetMap</a> contributors",
					sphericalMercator : true,
					wrapDateLine : true,
					tileOptions : null,
					initialize : function(name, url, options) {
						Tmap.Layer.XYZ.prototype.initialize.apply(this,
								arguments);
						this.tileOptions = Tmap.Util.extend({
							crossOriginKeyword : 'anonymous'
						}, this.options && this.options.tileOptions)
					},
					clone : function(obj) {
						if (obj == null) {
							obj = new Tmap.Layer.OSM(this.name, this.url, this
									.getOptions())
						}
						obj = Tmap.Layer.XYZ.prototype.clone.apply(this,
								[ obj ]);
						return obj
					},
					CLASS_NAME : "Tmap.Layer.OSM"
				});
Tmap.Layer.TMAP = Tmap.Class(Tmap.Layer.Grid, {
	serviceVersion : "1.0.0",
	layername : null,
	type : null,
	isBaseLayer : true,
	tileOrigin : null,
	serverResolutions : null,
	zoomOffset : 0,
	initialize : function(name, url, options) {
		var newArguments = [];
		newArguments.push(name, url, {}, options);
		Tmap.Layer.Grid.prototype.initialize.apply(this, newArguments)
	},
	clone : function(obj) {
		if (obj == null) {
			obj = new Tmap.Layer.TMS(this.name, this.url, this.getOptions())
		}
		obj = Tmap.Layer.Grid.prototype.clone.apply(this, [ obj ]);
		return obj
	},
	getURL : function(bounds) {
		bounds = this.adjustBounds(bounds);
		var res = this.getServerResolution();
		var x = Math.round((bounds.left - this.tileOrigin.lon)
				/ (res * this.tileSize.w));
		var y = Math.round((bounds.bottom - this.tileOrigin.lat)
				/ (res * this.tileSize.h));
		var z = this.serverResolutions != null ? Tmap.Util.indexOf(
				this.serverResolutions, res) : this.getServerZoom()
				+ this.zoomOffset;
		var path = this.serviceVersion + "/" + this.layername + "/" + z + "/"
				+ x + "/" + y + "." + this.type;
		var url = this.url;
		if (Tmap.Util.isArray(url)) {
			url = this.selectUrl(path, url)
		}
		x = 'C' + this.zeroPad(x, 8, 16);
		y = 'R' + this.zeroPad(y, 8, 16);
		z = 'L' + this.zeroPad(z, 2, 16);
		url = url + '/${z}/${y}/${x}.' + this.type;
		url = Tmap.String.format(url, {
			'x' : x,
			'y' : y,
			'z' : z
		});
		return Tmap.Util.urlAppend(url, Tmap.Util
				.getParameterString(this.params))
	},
	intToString : function(intValue) {
		if (intValue > 9) {
			return intValue.toString()
		} else {
			return "0" + intValue.toString()
		}
	},
	zeroPad : function(num, len, radix) {
		var str = num.toString(radix || 10);
		while (str.length < len) {
			str = "0" + str
		}
		return str
	},
	setMap : function(map) {
		Tmap.Layer.Grid.prototype.setMap.apply(this, arguments);
		if (!this.tileOrigin) {
			this.tileOrigin = new Tmap.LonLat(this.map.maxExtent.left,
					this.map.maxExtent.bottom)
		}
	},
	CLASS_NAME : "Tmap.Layer.TMS"
});
Tmap.Layer.TMS = Tmap.Class(Tmap.Layer.Grid, {
	serviceVersion : "1.0.0",
	layername : null,
	type : null,
	isBaseLayer : true,
	tileOrigin : null,
	serverResolutions : null,
	zoomOffset : 0,
	initialize : function(name, url, options) {
		var newArguments = [];
		newArguments.push(name, url, {}, options);
		Tmap.Layer.Grid.prototype.initialize.apply(this, newArguments)
	},
	clone : function(obj) {
		if (obj == null) {
			obj = new Tmap.Layer.TMS(this.name, this.url, this.getOptions())
		}
		obj = Tmap.Layer.Grid.prototype.clone.apply(this, [ obj ]);
		return obj
	},
	getURL : function(bounds) {
		bounds = this.adjustBounds(bounds);
		var res = this.getServerResolution();
		var x = Math.round((bounds.left - this.tileOrigin.lon)
				/ (res * this.tileSize.w));
		var y = Math.round((bounds.bottom - this.tileOrigin.lat)
				/ (res * this.tileSize.h));
		var z = this.getServerZoom();
		var path = this.serviceVersion + "/" + this.layername + "/" + z + "/"
				+ x + "/" + y + "." + this.type;
		var url = this.url;
		if (Tmap.Util.isArray(url)) {
			url = this.selectUrl(path, url)
		}
		return url + path
	},
	setMap : function(map) {
		Tmap.Layer.Grid.prototype.setMap.apply(this, arguments);
		if (!this.tileOrigin) {
			this.tileOrigin = new Tmap.LonLat(this.map.maxExtent.left,
					this.map.maxExtent.bottom)
		}
	},
	CLASS_NAME : "Tmap.Layer.TMS"
});
Tmap.Layer.TileCache = Tmap.Class(Tmap.Layer.Grid, {
	isBaseLayer : true,
	format : 'image/png',
	serverResolutions : null,
	initialize : function(name, url, layername, options) {
		this.layername = layername;
		Tmap.Layer.Grid.prototype.initialize.apply(this, [ name, url, {},
				options ]);
		this.extension = this.format.split('/')[1].toLowerCase();
		this.extension = (this.extension == 'jpg') ? 'jpeg' : this.extension
	},
	clone : function(obj) {
		if (obj == null) {
			obj = new Tmap.Layer.TileCache(this.name, this.url, this.layername,
					this.getOptions())
		}
		obj = Tmap.Layer.Grid.prototype.clone.apply(this, [ obj ]);
		return obj
	},
	getURL : function(bounds) {
		var res = this.getServerResolution();
		var bbox = this.maxExtent;
		var size = this.tileSize;
		var tileX = Math.round((bounds.left - bbox.left) / (res * size.w));
		var tileY = Math.round((bounds.bottom - bbox.bottom) / (res * size.h));
		var tileZ = this.serverResolutions != null ? Tmap.Util.indexOf(
				this.serverResolutions, res) : this.map.getZoom();
		var components = [
				this.layername,
				Tmap.Number.zeroPad(tileZ, 2),
				Tmap.Number.zeroPad(parseInt(tileX / 1000000), 3),
				Tmap.Number.zeroPad((parseInt(tileX / 1000) % 1000), 3),
				Tmap.Number.zeroPad((parseInt(tileX) % 1000), 3),
				Tmap.Number.zeroPad(parseInt(tileY / 1000000), 3),
				Tmap.Number.zeroPad((parseInt(tileY / 1000) % 1000), 3),
				Tmap.Number.zeroPad((parseInt(tileY) % 1000), 3) + '.'
						+ this.extension ];
		var path = components.join('/');
		var url = this.url;
		if (Tmap.Util.isArray(url)) {
			url = this.selectUrl(path, url)
		}
		url = (url.charAt(url.length - 1) == '/') ? url : url + '/';
		return url + path
	},
	CLASS_NAME : "Tmap.Layer.TileCache"
});
