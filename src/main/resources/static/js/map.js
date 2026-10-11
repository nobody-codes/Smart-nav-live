/**
 * SmartNav Live - Real-World Interactive Leaflet Map Engine
 * Powered by OpenStreetMap tiles, live GPS Geolocation, Nominatim Geocoding, and Real-Time Incident Reporting
 */

// Custom Leaflet Fullscreen Control
if (typeof L !== 'undefined' && !L.Control.Fullscreen) {
    L.Control.Fullscreen = L.Control.extend({
        options: {
            position: 'topleft',
            title: {
                'false': 'View Fullscreen',
                'true': 'Exit Fullscreen'
            }
        },
        onAdd: function (map) {
            const container = L.DomUtil.create('div', 'leaflet-control-fullscreen leaflet-bar leaflet-control');
            const link = L.DomUtil.create('a', 'leaflet-bar-part', container);
            link.href = '#';
            link.title = this.options.title['false'];
            link.setAttribute('role', 'button');
            link.setAttribute('aria-label', this.options.title['false']);
            link.innerHTML = '<i class="fa-solid fa-expand" style="display:flex;align-items:center;justify-content:center;height:100%;font-size:14px;color:#0f172a;"></i>';
            link.style.width = '34px';
            link.style.height = '34px';
            link.style.background = '#ffffff';
            link.style.cursor = 'pointer';

            L.DomEvent.on(link, 'click', L.DomEvent.stop)
                      .on(link, 'click', function () {
                          MapEngine.toggleFullscreen();
                      });

            const updateIcon = () => {
                const isFull = !!document.fullscreenElement;
                link.innerHTML = isFull 
                    ? '<i class="fa-solid fa-compress" style="display:flex;align-items:center;justify-content:center;height:100%;font-size:14px;color:#0f172a;"></i>'
                    : '<i class="fa-solid fa-expand" style="display:flex;align-items:center;justify-content:center;height:100%;font-size:14px;color:#0f172a;"></i>';
                link.title = isFull ? 'Exit Fullscreen' : 'View Fullscreen';
                setTimeout(() => map.invalidateSize(), 200);
            };

            document.addEventListener('fullscreenchange', updateIcon);
            return container;
        }
    });

    L.control.fullscreen = function (options) {
        return new L.Control.Fullscreen(options);
    };
}

const MapEngine = {
    map: null,
    clusterGroup: null,
    allReports: [],
    markersMap: new Map(),
    activeFilter: 'ALL',
    searchMarker: null,
    selectedMarker: null,
    userMarker: null,
    accuracyCircle: null,
    watchId: null,
    mySessionReportIds: new Set(),

    // Indian metropolitan quick-jump coordinates
    cities: {
        pune: { name: 'Pune', coords: [18.5204, 73.8567], zoom: 13 },
        mumbai: { name: 'Mumbai', coords: [19.0760, 72.8777], zoom: 12 },
        bengaluru: { name: 'Bengaluru', coords: [12.9716, 77.5946], zoom: 12 },
        delhi: { name: 'Delhi', coords: [28.6139, 77.2090], zoom: 12 },
        hyderabad: { name: 'Hyderabad', coords: [17.3850, 78.4867], zoom: 12 },
        chennai: { name: 'Chennai', coords: [13.0827, 80.2707], zoom: 12 },
        nashik: { name: 'Nashik', coords: [19.9975, 73.7898], zoom: 13 },
        nagpur: { name: 'Nagpur', coords: [21.1458, 79.0882], zoom: 13 }
    },

    categoryConfig: {
        ACCIDENT: { name: 'Accident', icon: 'fa-car-burst', color: '#ef4444', class: 'pin-accident' },
        TRAFFIC_JAM: { name: 'Traffic Jam', icon: 'fa-truck-moving', color: '#f59e0b', class: 'pin-traffic' },
        ROAD_BLOCK: { name: 'Road Block', icon: 'fa-ban', color: '#dc2626', class: 'pin-roadblock' },
        CONSTRUCTION: { name: 'Road Construction', icon: 'fa-person-digging', color: '#ea580c', class: 'pin-construction' },
        POTHOLE: { name: 'Pothole', icon: 'fa-triangle-exclamation', color: '#d97706', class: 'pin-pothole' },
        FLOOD: { name: 'Flooded Road', icon: 'fa-water', color: '#0284c7', class: 'pin-flood' },
        BROKEN_SIGNAL: { name: 'Broken Signal', icon: 'fa-traffic-light', color: '#8b5cf6', class: 'pin-signal' },
        VEHICLE_BREAKDOWN: { name: 'Vehicle Breakdown', icon: 'fa-car-side', color: '#e11d48', class: 'pin-breakdown' },
        FIRE: { name: 'Fire Hazard', icon: 'fa-fire', color: '#b91c1c', class: 'pin-fire' },
        POLICE_CHECKPOINT: { name: 'Police Checkpoint', icon: 'fa-shield-halved', color: '#3b82f6', class: 'pin-police' },
        OTHER: { name: 'Other Hazard', icon: 'fa-circle-exclamation', color: '#64748b', class: 'pin-other' }
    },

    init: function (elementId = 'mainMap', options = {}) {
        const el = document.getElementById(elementId);
        if (!el) return;

        // Default center: Pune, Maharashtra, India [18.5204, 73.8567]
        const defaultCenter = options.center || this.cities.pune.coords;
        const defaultZoom = options.zoom || 13;

        // Initialize Real Leaflet Map with Pan and Zoom enabled
        this.map = L.map(elementId, {
            zoomControl: false,
            attributionControl: true,
            scrollWheelZoom: true,
            dragging: true
        }).setView(defaultCenter, defaultZoom);

        // Standard OpenStreetMap Tiles (Real roads, buildings, streets, highways)
        const osmStandard = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
            maxZoom: 19
        });

        // Additional Navigational & Dark Basemaps
        const cartoVoyager = L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
            attribution: '&copy; OpenStreetMap &copy; CARTO',
            subdomains: 'abcd',
            maxZoom: 19
        });

        const cartoDark = L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
            attribution: '&copy; OpenStreetMap &copy; CARTO',
            subdomains: 'abcd',
            maxZoom: 19
        });

        // Set OpenStreetMap standard tiles as the default active layer
        osmStandard.addTo(this.map);

        const baseMaps = {
            "🗺️ OpenStreetMap (Default)": osmStandard,
            "🏙️ Voyager Navigation": cartoVoyager,
            "🌙 Dark Mode": cartoDark
        };

        if (options.showLayerControl !== false) {
            L.control.layers(baseMaps, null, { position: 'topright' }).addTo(this.map);
        }

        // Add Zoom Control
        L.control.zoom({ position: 'bottomright' }).addTo(this.map);

        // Add Fullscreen Control to Leaflet controls
        if (options.showFullscreenControl !== false && L.control.fullscreen) {
            L.control.fullscreen({ position: 'topleft' }).addTo(this.map);
        }

        // Invalidate map size after DOM render
        setTimeout(() => { if (this.map) this.map.invalidateSize(); }, 300);

        // Initialize Marker Cluster Group for incident density management
        this.clusterGroup = L.markerClusterGroup({
            showCoverageOnHover: false,
            maxClusterRadius: 40,
            iconCreateFunction: function (cluster) {
                const count = cluster.getChildCount();
                return L.divIcon({
                    html: `<div class="custom-incident-pin" style="background:#06b6d4; color:#fff; font-weight:800; border:2px solid #fff;">${count}</div>`,
                    className: 'marker-cluster-custom',
                    iconSize: L.point(40, 40)
                });
            }
        });
        this.map.addLayer(this.clusterGroup);

        // Load initial reports from database
        this.loadReports();

        // Listen for real-time WebSocket updates
        if (window.SmartNav) {
            SmartNav.registerReportListener(event => this.handleLiveReportUpdate(event));
        }

        // Freshness polling every 10 seconds
        setInterval(() => this.loadReports(true), 10000);

        // Setup map click event for selecting coordinates and reporting
        if (options.enableClickToReport !== false) {
            this.map.on('click', e => this.handleMapClick(e));
        }

        this.bindUiControls();
        this.setupAutocompleteSearch();
    },

    toggleFullscreen: function () {
        const container = document.querySelector('.map-container-fullscreen') || document.getElementById('mainMap');
        if (!container) return;

        if (!document.fullscreenElement) {
            if (container.requestFullscreen) {
                container.requestFullscreen();
            } else if (container.webkitRequestFullscreen) {
                container.webkitRequestFullscreen();
            } else if (container.msRequestFullscreen) {
                container.msRequestFullscreen();
            }
        } else {
            if (document.exitFullscreen) {
                document.exitFullscreen();
            } else if (document.webkitExitFullscreen) {
                document.webkitExitFullscreen();
            }
        }
        setTimeout(() => { if (this.map) this.map.invalidateSize(); }, 250);
    },

    flyToCity: function (cityName) {
        const city = this.cities[cityName.toLowerCase()];
        if (city && this.map) {
            this.map.flyTo(city.coords, city.zoom, { duration: 1.5 });
            SmartNav?.showToast?.({
                title: 'Map Relocated',
                message: `Viewing real OpenStreetMap data in ${city.name}, India`,
                type: 'info'
            });
        }
    },

    loadReports: function (silent = false) {
        fetch('/api/reports')
            .then(res => res.json())
            .then(res => {
                if (res.success && res.data) {
                    this.allReports = res.data;
                    this.renderMarkers();
                }
            })
            .catch(err => {
                if (!silent) console.error('Failed to load reports', err);
            });
    },

    renderMarkers: function () {
        this.clusterGroup.clearLayers();
        this.markersMap.clear();

        // Display all approved traffic incidents at their real coordinates
        // (plus any newly submitted incidents created in the current user session)
        const filtered = this.allReports.filter(r => {
            const isApproved = (r.status === 'APPROVED');
            const isSessionCreated = this.mySessionReportIds && this.mySessionReportIds.has(r.id);
            if (!isApproved && !isSessionCreated) return false;

            if (this.activeFilter === 'ALL') return true;
            return r.category === this.activeFilter;
        });

        filtered.forEach(report => {
            this.addMarkerForReport(report);
        });
    },

    addMarkerForReport: function (report) {
        if (!report.latitude || !report.longitude) return;

        const config = this.categoryConfig[report.category] || {
            name: report.category ? report.category.replace(/_/g, ' ') : 'Hazard',
            icon: 'fa-triangle-exclamation',
            color: '#10b981',
            class: 'pin-traffic'
        };

        const pinHtml = `
            <div class="custom-incident-pin ${config.class}" title="${config.name}">
                <i class="fa-solid ${config.icon} text-white" style="font-size: 15px;"></i>
            </div>
        `;

        const customIcon = L.divIcon({
            html: pinHtml,
            className: 'custom-leaflet-marker',
            iconSize: [36, 36],
            iconAnchor: [18, 18],
            popupAnchor: [0, -20]
        });

        const marker = L.marker([report.latitude, report.longitude], { icon: customIcon });

        const statusBadgeClass = report.status === 'APPROVED' ? 'badge-status-approved' :
                                (report.status === 'PENDING' ? 'badge-status-pending' : 'badge-status-rejected');

        const imageHtml = report.image ? `
            <div class="my-2 rounded overflow-hidden" style="max-height: 140px;">
                <img src="${report.image}" alt="${config.name}" class="img-fluid w-100 object-fit-cover" 
                     onerror="this.style.display='none'">
            </div>
        ` : '';

        const cityState = (report.city ? report.city : '') + (report.state ? ', ' + report.state : '');
        const formattedTime = this.formatDateTime(report.createdAt);

        // Popup displaying incident type, description, address, and time
        const popupHtml = `
            <div class="smartnav-popup p-2" style="min-width: 260px; max-width: 320px; color: #0f172a;">
                <div class="d-flex justify-content-between align-items-center mb-1">
                    <span class="badge ${statusBadgeClass}">${report.status}</span>
                    <small class="text-muted"><i class="fa-solid fa-clock me-1"></i>${formattedTime}</small>
                </div>
                <h6 class="fw-bold mb-1 text-dark" style="font-size: 0.95rem;">
                    <i class="fa-solid ${config.icon} me-1" style="color: ${config.color};"></i>
                    ${config.name}
                </h6>
                <div class="text-secondary small mb-1">
                    <i class="fa-solid fa-location-dot me-1 text-danger"></i><b>Address:</b> ${report.address || 'Real location'}
                    ${cityState ? `<br><small class="text-muted ps-3">${cityState}</small>` : ''}
                </div>
                <div class="small text-dark mb-2 bg-light p-2 rounded border border-light-subtle">
                    <b>Description:</b> ${report.description}
                </div>
                ${imageHtml}
                <div class="d-flex justify-content-between align-items-center pt-2 border-top border-secondary-subtle">
                    <span class="badge bg-secondary-subtle text-dark">Severity: ${report.severity || 'MEDIUM'}</span>
                    <small class="text-muted"><i class="fa-solid fa-calendar me-1"></i>${formattedTime}</small>
                </div>
                <div class="mt-2 pt-2 border-top border-secondary-subtle d-flex gap-2">
                    <a href="/routes?destLat=${report.latitude}&destLng=${report.longitude}&destName=${encodeURIComponent(report.address || 'Hazard Location')}" 
                       class="btn btn-sm btn-cyan-gradient w-100 py-1" style="font-size: 0.75rem;">
                        <i class="fa-solid fa-route me-1"></i> Plan Route Here
                    </a>
                </div>
            </div>
        `;

        marker.bindPopup(popupHtml);
        this.clusterGroup.addLayer(marker);
        this.markersMap.set(report.id, marker);
    },

    handleLiveReportUpdate: function (event) {
        if (event.action === 'NEW_REPORT' && event.report) {
            this.allReports.unshift(event.report);
            if (this.activeFilter === 'ALL' || this.activeFilter === event.report.category) {
                this.addMarkerForReport(event.report);
            }
        } else if (event.action === 'STATUS_CHANGE' && event.report) {
            const index = this.allReports.findIndex(r => r.id === event.report.id);
            if (index !== -1) {
                this.allReports[index] = event.report;
            }
            this.renderMarkers();
        } else if (event.action === 'REPORT_DELETED') {
            this.allReports = this.allReports.filter(r => r.id !== event.reportId);
            this.renderMarkers();
        }
    },

    handleMapClick: function (e) {
        const lat = e.latlng.lat;
        const lng = e.latlng.lng;

        const latInput = document.getElementById('reportLat');
        const lngInput = document.getElementById('reportLng');
        const addrInput = document.getElementById('reportAddress');

        if (latInput) latInput.value = lat.toFixed(6);
        if (lngInput) lngInput.value = lng.toFixed(6);

        // Place a visual marker on clicked coordinates
        if (this.selectedMarker) {
            this.map.removeLayer(this.selectedMarker);
        }

        const clickIcon = L.divIcon({
            html: '<div class="custom-incident-pin" style="background:#06b6d4; border:2px solid #ffffff; box-shadow:0 0 14px rgba(6,182,212,0.8);"><i class="fa-solid fa-location-pin text-white" style="font-size:16px;"></i></div>',
            iconSize: [36, 36],
            iconAnchor: [18, 18],
            popupAnchor: [0, -18]
        });

        this.selectedMarker = L.marker([lat, lng], { icon: clickIcon }).addTo(this.map);

        const initialPopupContent = `
            <div class="p-2 text-dark" style="min-width: 230px;">
                <h6 class="fw-bold mb-1"><i class="fa-solid fa-crosshairs text-info me-1"></i>Selected Coordinates</h6>
                <div class="small text-secondary mb-1">
                    <b>Latitude:</b> ${lat.toFixed(6)}<br>
                    <b>Longitude:</b> ${lng.toFixed(6)}
                </div>
                <div id="clickMarkerAddressText" class="small text-muted mb-2"><i class="fa-solid fa-spinner fa-spin me-1"></i>Resolving address via Nominatim...</div>
                <div class="d-flex flex-column gap-1">
                    <button class="btn btn-sm btn-primary-smartnav w-100 py-1" onclick="MapEngine.openReportModal(${lat}, ${lng})">
                        <i class="fa-solid fa-triangle-exclamation me-1"></i> Report Incident at Pin
                    </button>
                    <button class="btn btn-sm btn-outline-info text-dark w-100 py-1" onclick="MapEngine.quickSaveCoordinates(${lat}, ${lng})">
                        <i class="fa-solid fa-floppy-disk me-1 text-primary"></i> Store Coordinates in DB
                    </button>
                </div>
            </div>
        `;
        this.selectedMarker.bindPopup(initialPopupContent).openPopup();

        // Reverse geocode clicked coordinates via backend OpenStreetMap Nominatim API
        fetch(`/api/location/reverse?lat=${lat}&lng=${lng}`)
            .then(res => res.json())
            .then(res => {
                let resolvedName = `Location at (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
                if (res.success && res.data) {
                    resolvedName = res.data.displayName || res.data.name;
                    if (addrInput) addrInput.value = resolvedName;
                    const cityInput = document.getElementById('reportCity');
                    const stateInput = document.getElementById('reportState');
                    if (cityInput && res.data.city) cityInput.value = res.data.city;
                    if (stateInput && res.data.state) stateInput.value = res.data.state;
                }
                const addrEl = document.getElementById('clickMarkerAddressText');
                if (addrEl) addrEl.textContent = resolvedName;
            })
            .catch(() => {
                if (addrInput && !addrInput.value) {
                    addrInput.value = `Location at (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
                }
                const addrEl = document.getElementById('clickMarkerAddressText');
                if (addrEl) addrEl.textContent = `Coordinates (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
            });
    },

    openReportModal: function (lat, lng, address) {
        const latInput = document.getElementById('reportLat');
        const lngInput = document.getElementById('reportLng');
        const addrInput = document.getElementById('reportAddress');

        if (latInput) latInput.value = Number(lat).toFixed(6);
        if (lngInput) lngInput.value = Number(lng).toFixed(6);
        if (address && addrInput) addrInput.value = address;

        const reportModalEl = document.getElementById('reportIncidentModal');
        if (reportModalEl) {
            const modal = bootstrap.Modal.getOrCreateInstance(reportModalEl);
            modal.show();
        }
    },

    quickSaveCoordinates: function (lat, lng) {
        const addrInput = document.getElementById('reportAddress');
        const address = (addrInput && addrInput.value) ? addrInput.value : `Selected Location (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
        const cityInput = document.getElementById('reportCity');
        const stateInput = document.getElementById('reportState');

        const payload = {
            category: 'OTHER',
            severity: 'MEDIUM',
            address: address,
            city: (cityInput && cityInput.value) ? cityInput.value : 'Pune',
            state: (stateInput && stateInput.value) ? stateInput.value : 'Maharashtra',
            country: 'India',
            latitude: Number(lat),
            longitude: Number(lng),
            description: `Crowdsourced waypoint stored at real coordinates (${Number(lat).toFixed(6)}, ${Number(lng).toFixed(6)}).`
        };

        fetch('/api/reports', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
        .then(res => res.json())
        .then(res => {
            if (res.success && res.data) {
                if (!this.mySessionReportIds) this.mySessionReportIds = new Set();
                this.mySessionReportIds.add(res.data.id);
                this.allReports.unshift(res.data);
                this.addMarkerForReport(res.data);

                SmartNav?.showToast?.({
                    title: 'Coordinates Saved to Database! 💾',
                    message: `Real location (${lat.toFixed(4)}, ${lng.toFixed(4)}) recorded in database.`,
                    type: 'success'
                });

                if (this.selectedMarker) {
                    this.map.removeLayer(this.selectedMarker);
                    this.selectedMarker = null;
                }
            } else {
                alert(res.message || 'Error storing coordinates in database.');
            }
        })
        .catch(err => {
            console.error('Failed to store coordinates in database', err);
            alert('Failed to store coordinates in database. See console for details.');
        });
    },

    locateUser: function () {
        if (!navigator.geolocation) {
            alert('Geolocation is not supported by your browser.');
            return;
        }

        const locateBtn = document.getElementById('btnLocateMe');
        const useMyLocBtn = document.getElementById('btnUseMyLocation');
        if (locateBtn) {
            locateBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin fs-5 text-primary"></i>';
        }
        if (useMyLocBtn) {
            useMyLocBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-2"></i>Locating GPS...';
        }

        navigator.geolocation.getCurrentPosition(pos => {
            const lat = pos.coords.latitude;
            const lng = pos.coords.longitude;
            const accuracy = pos.coords.accuracy;

            this.updateUserPosition(lat, lng, accuracy);
            this.map.flyTo([lat, lng], 16, { duration: 1.5 });

            if (locateBtn) {
                locateBtn.innerHTML = '<i class="fa-solid fa-location-arrow fs-5 text-success"></i>';
                locateBtn.title = 'Live GPS Tracking Active';
            }
            if (useMyLocBtn) {
                useMyLocBtn.innerHTML = '<i class="fa-solid fa-location-arrow text-success me-1"></i> Location Locked (±' + Math.round(accuracy) + 'm)';
            }

            SmartNav?.showToast?.({
                title: 'GPS Position Locked',
                message: `Located within ±${Math.round(accuracy)}m. Live tracking active.`,
                type: 'success'
            });

            if (!this.watchId) {
                this.watchId = navigator.geolocation.watchPosition(
                    watchPos => {
                        this.updateUserPosition(
                            watchPos.coords.latitude,
                            watchPos.coords.longitude,
                            watchPos.coords.accuracy
                        );
                    },
                    err => console.warn('GPS continuous update warning:', err),
                    { enableHighAccuracy: true, maximumAge: 5000, timeout: 10000 }
                );
            }
        }, err => {
            console.warn('Geolocation failed or denied', err);
            if (locateBtn) {
                locateBtn.innerHTML = '<i class="fa-solid fa-crosshairs fs-5 text-primary"></i>';
            }
            if (useMyLocBtn) {
                useMyLocBtn.innerHTML = '<i class="fa-solid fa-crosshairs text-info"></i><span class="fw-bold ms-1">Use My Location</span>';
            }
            SmartNav?.showToast?.({
                title: 'GPS Location Notice',
                message: 'Device location permission denied or timed out. Map centered on Pune, Maharashtra.',
                type: 'info'
            });
        }, { enableHighAccuracy: true, timeout: 8000 });
    },

    updateUserPosition: function (lat, lng, accuracy) {
        if (!this.userMarker) {
            const userIcon = L.divIcon({
                html: '<div class="custom-incident-pin" style="background:#10b981; border:3px solid #ffffff; box-shadow:0 0 14px rgba(16,185,129,0.8);"><i class="fa-solid fa-location-arrow text-white" style="font-size:16px;"></i></div>',
                className: 'user-gps-marker',
                iconSize: [38, 38],
                iconAnchor: [19, 19]
            });
            this.userMarker = L.marker([lat, lng], { icon: userIcon, zIndexOffset: 1000 }).addTo(this.map);
            this.userMarker.bindPopup('<b>Your Current Location</b><br><small class="text-muted">Live crowdsourced GPS node</small>');
        } else {
            this.userMarker.setLatLng([lat, lng]);
        }

        if (accuracy && accuracy < 500) {
            if (!this.accuracyCircle) {
                this.accuracyCircle = L.circle([lat, lng], {
                    radius: accuracy,
                    color: '#10b981',
                    fillColor: '#10b981',
                    fillOpacity: 0.12,
                    weight: 1
                }).addTo(this.map);
            } else {
                this.accuracyCircle.setLatLng([lat, lng]);
                this.accuracyCircle.setRadius(accuracy);
            }
        }
    },

    setupAutocompleteSearch: function () {
        const input = document.getElementById('mapSearchInput');
        if (!input) return;

        let debounceTimer;
        let dropdown = document.createElement('div');
        dropdown.className = 'autocomplete-dropdown d-none';
        input.parentElement.style.position = 'relative';
        input.parentElement.appendChild(dropdown);

        input.addEventListener('input', () => {
            clearTimeout(debounceTimer);
            const query = input.value.trim();
            if (query.length < 2) {
                dropdown.classList.add('d-none');
                return;
            }

            debounceTimer = setTimeout(() => {
                fetch(`/api/location/search?q=${encodeURIComponent(query)}`)
                    .then(res => res.json())
                    .then(res => {
                        if (res.success && res.data && res.data.length > 0) {
                            dropdown.innerHTML = '';
                            res.data.forEach(item => {
                                const opt = document.createElement('div');
                                opt.className = 'autocomplete-item';
                                opt.innerHTML = `
                                    <div class="fw-bold text-white small"><i class="fa-solid fa-location-dot text-primary me-2"></i>${item.name}</div>
                                    <div class="text-secondary small text-truncate" style="font-size: 0.72rem;">${item.displayName}</div>
                                `;
                                opt.addEventListener('click', () => {
                                    input.value = item.name;
                                    dropdown.classList.add('d-none');
                                    this.jumpToLocation(item.lat, item.lng, item.name, item.displayName);
                                });
                                dropdown.appendChild(opt);
                            });
                            dropdown.classList.remove('d-none');
                        } else {
                            dropdown.classList.add('d-none');
                        }
                    })
                    .catch(() => dropdown.classList.add('d-none'));
            }, 250);
        });

        // Hide dropdown when clicking outside
        document.addEventListener('click', (e) => {
            if (!input.parentElement.contains(e.target)) {
                dropdown.classList.add('d-none');
            }
        });
    },

    jumpToLocation: function (lat, lng, name, displayName) {
        this.map.flyTo([lat, lng], 15, { duration: 1.5 });

        if (this.searchMarker) {
            this.map.removeLayer(this.searchMarker);
        }

        const icon = L.divIcon({
            html: '<div class="custom-incident-pin" style="background:#6366f1; border:2px solid #fff; box-shadow:0 0 12px rgba(99,102,241,0.8);"><i class="fa-solid fa-magnifying-glass-location text-white"></i></div>',
            iconSize: [36, 36],
            iconAnchor: [18, 18],
            popupAnchor: [0, -18]
        });

        const popupContent = `
            <div class="p-2" style="color: #0f172a; min-width: 220px;">
                <h6 class="fw-bold mb-1">${name}</h6>
                <div class="small text-muted mb-2">${displayName || ''}</div>
                <div class="d-flex flex-column gap-1">
                    <a href="/routes?startLat=${lat}&startLng=${lng}&startName=${encodeURIComponent(name)}" class="btn btn-sm btn-primary-smartnav text-white py-1" style="font-size:0.75rem;">
                        <i class="fa-solid fa-play me-1"></i> Start Route Here
                    </a>
                    <a href="/routes?destLat=${lat}&destLng=${lng}&destName=${encodeURIComponent(name)}" class="btn btn-sm btn-cyan-gradient py-1" style="font-size:0.75rem;">
                        <i class="fa-solid fa-flag-checkered me-1"></i> Destination Here
                    </a>
                    <button class="btn btn-sm btn-outline-primary py-1" style="font-size:0.75rem;" onclick="MapEngine.openReportModal(${lat}, ${lng}, '${name.replace(/'/g, "\\'")}')">
                        <i class="fa-solid fa-plus me-1"></i> Report Incident Here
                    </button>
                </div>
            </div>
        `;

        this.searchMarker = L.marker([lat, lng], { icon: icon }).addTo(this.map).bindPopup(popupContent).openPopup();
    },

    searchLocation: function (query) {
        if (!query || !query.trim()) return;

        fetch(`/api/location/search?q=${encodeURIComponent(query)}`)
            .then(res => res.json())
            .then(res => {
                if (res.success && res.data && res.data.length > 0) {
                    const first = res.data[0];
                    this.jumpToLocation(first.lat, first.lng, first.name, first.displayName);
                } else {
                    SmartNav?.showToast?.({
                        title: 'Location Search',
                        message: 'No matching real-world location found. Please refine place name.',
                        type: 'warning'
                    });
                }
            })
            .catch(err => console.error('Geocoding search failed', err));
    },

    filterByCategory: function (category) {
        this.activeFilter = category;
        this.renderMarkers();

        // Update active class on filter buttons
        document.querySelectorAll('.filter-category-btn').forEach(btn => {
            if (btn.dataset.category === category) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });
    },

    bindUiControls: function () {
        // Use My Location Buttons
        const locateBtn = document.getElementById('btnLocateMe');
        if (locateBtn) {
            locateBtn.addEventListener('click', () => this.locateUser());
        }
        const useMyLocationBtn = document.getElementById('btnUseMyLocation');
        if (useMyLocationBtn) {
            useMyLocationBtn.addEventListener('click', () => this.locateUser());
        }

        // Fullscreen Button
        const fullscreenBtn = document.getElementById('btnFullscreen');
        if (fullscreenBtn) {
            fullscreenBtn.addEventListener('click', () => this.toggleFullscreen());
        }

        // Search Input & Button
        const searchInput = document.getElementById('mapSearchInput');
        const searchBtn = document.getElementById('mapSearchBtn');
        if (searchBtn && searchInput) {
            searchBtn.addEventListener('click', () => this.searchLocation(searchInput.value));
            searchInput.addEventListener('keypress', e => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    this.searchLocation(searchInput.value);
                }
            });
        }

        // City Quick-Jump Buttons
        document.querySelectorAll('.city-jump-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const city = btn.dataset.city;
                if (city) this.flyToCity(city);
                document.querySelectorAll('.city-jump-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
            });
        });

        // Category Filter Buttons
        document.querySelectorAll('.filter-category-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                this.filterByCategory(btn.dataset.category);
            });
        });
    },

    formatDateTime: function (dateStr) {
        if (!dateStr) return 'Recently';
        try {
            const d = new Date(dateStr);
            if (isNaN(d.getTime())) return dateStr;
            return d.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' }) + ', ' +
                   d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        } catch {
            return dateStr;
        }
    }
};

window.MapEngine = MapEngine;
