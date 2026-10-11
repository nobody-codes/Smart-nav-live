/**
 * SmartNav Live - Intelligent Route Planning & Dynamic Hazard Avoidance
 */

const RouteEngine = {
    map: null,
    currentRouteLayer: null,
    congestedRouteLayer: null,
    startMarker: null,
    destMarker: null,
    hazardMarkers: [],
    pickingTarget: null, // 'source' or 'dest'
    lastCalculationResponse: null,

    // Preset real-world urban landmarks for instant demonstration
    presets: {
        pune_station_hinjewadi: {
            source: { name: 'Pune Junction Railway Station', lat: 18.5289, lng: 73.8744 },
            dest: { name: 'Hinjewadi Phase 1, Pune', lat: 18.5913, lng: 73.7389 }
        },
        pune_swargate_airport: {
            source: { name: 'Swargate, Pune', lat: 18.5018, lng: 73.8586 },
            dest: { name: 'Pune Airport / Viman Nagar', lat: 18.5679, lng: 73.9143 }
        },
        mumbai_airport_bkc: {
            source: { name: 'Mumbai Airport (T2)', lat: 19.0974, lng: 72.8744 },
            dest: { name: 'Bandra Kurla Complex (BKC), Mumbai', lat: 19.0660, lng: 72.8687 }
        },
        bengaluru_majestic_silkboard: {
            source: { name: 'KSR Bengaluru Majestic', lat: 12.9784, lng: 77.5726 },
            dest: { name: 'Silk Board Junction, Bengaluru', lat: 12.9172, lng: 77.6228 }
        },
        delhi_cp_indiagate: {
            source: { name: 'Connaught Place, New Delhi', lat: 28.6315, lng: 77.2167 },
            dest: { name: 'India Gate, New Delhi', lat: 28.5672, lng: 77.2100 }
        }
    },

    altRouteLayer: null,
    incidentClusterGroup: null,

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

    init: function (elementId = 'routeMap') {
        const el = document.getElementById(elementId);
        if (!el) return;

        // Centered on Pune, Maharashtra, India [18.5204, 73.8567]
        this.map = L.map(elementId, { 
            zoomControl: false,
            scrollWheelZoom: true,
            dragging: true 
        }).setView([18.5204, 73.8567], 12);

        // Real OpenStreetMap Standard Tiles (Default)
        const osmStandard = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
            maxZoom: 19
        });

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

        const openTopo = L.tileLayer('https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; OpenStreetMap &copy; OpenTopoMap',
            maxZoom: 17
        });

        // Set OpenStreetMap standard tiles as the default active layer
        osmStandard.addTo(this.map);

        const baseMaps = {
            "🗺️ OpenStreetMap": osmStandard,
            "🏙️ Voyager (Nav)": cartoVoyager,
            "🌙 Dark Mode": cartoDark,
            "⛰️ Topographic": openTopo
        };

        L.control.layers(baseMaps, null, { position: 'topright' }).addTo(this.map);
        L.control.zoom({ position: 'bottomright' }).addTo(this.map);

        // Invalidate map size to guarantee full card rendering
        setTimeout(() => { if (this.map) this.map.invalidateSize(); }, 300);
        window.addEventListener('resize', () => { if (this.map) this.map.invalidateSize(); });

        this.map.on('click', e => this.handleMapClick(e));
        this.setupAutocomplete('sourceName', 'sourceAutocomplete', 'sourceLat', 'sourceLng', true);
        this.setupAutocomplete('destName', 'destAutocomplete', 'destLat', 'destLng', false);
        this.bindEvents();

        // Load approved traffic incident markers across the route map
        this.loadApprovedIncidents();

        // Preload default demonstration route (Pune Station -> Hinjewadi Phase 1)
        this.applyPreset('pune_station_hinjewadi');
        this.calculateRoute();

        // Listen for live incidents to prompt dynamic recalculation
        if (window.SmartNav) {
            SmartNav.registerReportListener(event => {
                if (event.action === 'NEW_REPORT' || event.action === 'STATUS_CHANGE') {
                    this.loadApprovedIncidents();
                    const notice = document.getElementById('recalculateAlert');
                    if (notice) notice.classList.remove('d-none');
                }
            });
        }
    },

    loadApprovedIncidents: function () {
        fetch('/api/reports')
            .then(res => res.json())
            .then(res => {
                if (res.success && res.data) {
                    if (!this.incidentClusterGroup) {
                        this.incidentClusterGroup = L.markerClusterGroup({
                            showCoverageOnHover: false,
                            maxClusterRadius: 35,
                            iconCreateFunction: function (cluster) {
                                const count = cluster.getChildCount();
                                return L.divIcon({
                                    html: `<div class="custom-incident-pin" style="background:#06b6d4; color:#fff; font-weight:800; border:2px solid #fff;">${count}</div>`,
                                    className: 'marker-cluster-custom',
                                    iconSize: L.point(34, 34)
                                });
                            }
                        });
                        this.map.addLayer(this.incidentClusterGroup);
                    } else {
                        this.incidentClusterGroup.clearLayers();
                    }

                    const approved = res.data.filter(r => r.status === 'APPROVED');
                    approved.forEach(report => {
                        if (!report.latitude || !report.longitude) return;
                        const iconConfig = this.categoryConfig[report.category] || {
                            name: report.category ? report.category.replace(/_/g, ' ') : 'Hazard',
                            icon: 'fa-triangle-exclamation',
                            color: '#f59e0b',
                            class: 'pin-traffic'
                        };

                        const customIcon = L.divIcon({
                            html: `<div class="custom-incident-pin ${iconConfig.class}" title="${iconConfig.name}"><i class="fa-solid ${iconConfig.icon} text-white" style="font-size: 13px;"></i></div>`,
                            className: 'custom-leaflet-marker',
                            iconSize: [32, 32],
                            iconAnchor: [16, 16],
                            popupAnchor: [0, -16]
                        });

                        const marker = L.marker([report.latitude, report.longitude], { icon: customIcon });
                        const timeStr = report.createdAt ? new Date(report.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Active';
                        marker.bindPopup(`
                            <div class="p-2 text-dark" style="min-width: 220px; max-width: 280px;">
                                <div class="d-flex justify-content-between align-items-center mb-1">
                                    <span class="badge bg-success-subtle text-success">APPROVED</span>
                                    <small class="text-muted"><i class="fa-solid fa-clock me-1"></i>${timeStr}</small>
                                </div>
                                <h6 class="fw-bold mb-1"><i class="fa-solid ${iconConfig.icon} me-1" style="color:${iconConfig.color};"></i>${iconConfig.name}</h6>
                                <p class="small text-secondary mb-1"><i class="fa-solid fa-location-dot me-1 text-danger"></i>${report.address || 'Real location'}</p>
                                <div class="small bg-light p-2 rounded border border-light-subtle mb-2">${report.description || ''}</div>
                                <div class="d-flex justify-content-between align-items-center pt-1 border-top small text-muted">
                                    <span>Severity: <b>${report.severity || 'MEDIUM'}</b></span>
                                </div>
                            </div>
                        `);
                        this.incidentClusterGroup.addLayer(marker);
                    });
                }
            })
            .catch(err => console.warn('Failed to load incident markers on route map', err));
    },

    applyPreset: function (key) {
        const p = this.presets[key];
        if (!p) return;
        document.getElementById('sourceName').value = p.source.name;
        document.getElementById('sourceLat').value = p.source.lat;
        document.getElementById('sourceLng').value = p.source.lng;
        this.updateStartMarker(p.source.lat, p.source.lng, p.source.name);

        document.getElementById('destName').value = p.dest.name;
        document.getElementById('destLat').value = p.dest.lat;
        document.getElementById('destLng').value = p.dest.lng;
        this.updateDestMarker(p.dest.lat, p.dest.lng, p.dest.name);
    },

    setupAutocomplete: function (inputId, dropdownId, latId, lngId, isSource) {
        const input = document.getElementById(inputId);
        const dropdown = document.getElementById(dropdownId);
        if (!input || !dropdown) return;

        let debounceTimer;
        input.addEventListener('input', (e) => {
            clearTimeout(debounceTimer);
            const q = e.target.value.trim();
            if (q.length < 2) {
                dropdown.classList.add('d-none');
                dropdown.innerHTML = '';
                return;
            }
            debounceTimer = setTimeout(() => {
                fetch(`/api/location/search?query=${encodeURIComponent(q)}`)
                    .then(res => res.json())
                    .then(res => {
                        if (res.success && res.data && res.data.length > 0) {
                            dropdown.innerHTML = '';
                            res.data.forEach(item => {
                                const div = document.createElement('div');
                                div.className = 'autocomplete-item';
                                div.innerHTML = `<i class="fa-solid fa-location-dot text-primary me-2"></i><strong>${item.name}</strong><br><small class="text-secondary">${item.displayName || ''}</small>`;
                                div.addEventListener('click', () => {
                                    const lat = item.lat != null ? item.lat : (item.latitude != null ? item.latitude : 0);
                                    const lng = item.lng != null ? item.lng : (item.longitude != null ? item.longitude : 0);
                                    input.value = item.name || item.displayName || 'Selected Location';
                                    document.getElementById(latId).value = Number(lat).toFixed(6);
                                    document.getElementById(lngId).value = Number(lng).toFixed(6);
                                    dropdown.classList.add('d-none');
                                    if (isSource) {
                                        RouteEngine.updateStartMarker(lat, lng, item.name);
                                    } else {
                                        RouteEngine.updateDestMarker(lat, lng, item.name);
                                    }
                                    RouteEngine.map.panTo([lat, lng]);
                                });
                                dropdown.appendChild(div);
                            });
                            dropdown.classList.remove('d-none');
                        } else {
                            dropdown.classList.add('d-none');
                        }
                    })
                    .catch(() => dropdown.classList.add('d-none'));
            }, 300);
        });

        document.addEventListener('click', (e) => {
            if (!input.contains(e.target) && !dropdown.contains(e.target)) {
                dropdown.classList.add('d-none');
            }
        });
    },

    handleMapClick: function (e) {
        const lat = e.latlng.lat;
        const lng = e.latlng.lng;

        if (this.pickingTarget === 'source') {
            document.getElementById('sourceLat').value = lat.toFixed(6);
            document.getElementById('sourceLng').value = lng.toFixed(6);
            document.getElementById('sourceName').value = `Selected Point (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
            this.updateStartMarker(lat, lng, 'Start');
            this.pickingTarget = null;
            document.getElementById('pickSourceBtn')?.classList.remove('active');
        } else if (this.pickingTarget === 'dest') {
            document.getElementById('destLat').value = lat.toFixed(6);
            document.getElementById('destLng').value = lng.toFixed(6);
            document.getElementById('destName').value = `Selected Point (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
            this.updateDestMarker(lat, lng, 'Destination');
            this.pickingTarget = null;
            document.getElementById('pickDestBtn')?.classList.remove('active');
        }
    },

    updateStartMarker: function (lat, lng, label) {
        if (this.startMarker) this.map.removeLayer(this.startMarker);
        const icon = L.divIcon({
            html: '<div class="custom-incident-pin" style="background:#10b981;"><i class="fa-solid fa-play text-white"></i></div>',
            iconSize: [36, 36],
            iconAnchor: [18, 18]
        });
        this.startMarker = L.marker([lat, lng], { icon: icon }).addTo(this.map).bindPopup(`<b>Start:</b> ${label}`);
    },

    updateDestMarker: function (lat, lng, label) {
        if (this.destMarker) this.map.removeLayer(this.destMarker);
        const icon = L.divIcon({
            html: '<div class="custom-incident-pin" style="background:#3b82f6;"><i class="fa-solid fa-flag-checkered text-white"></i></div>',
            iconSize: [36, 36],
            iconAnchor: [18, 18]
        });
        this.destMarker = L.marker([lat, lng], { icon: icon }).addTo(this.map).bindPopup(`<b>Destination:</b> ${label}`);
    },

    calculateRoute: function () {
        const sLat = parseFloat(document.getElementById('sourceLat').value);
        const sLng = parseFloat(document.getElementById('sourceLng').value);
        const sName = document.getElementById('sourceName').value || 'Starting Point';
        const dLat = parseFloat(document.getElementById('destLat').value);
        const dLng = parseFloat(document.getElementById('destLng').value);
        const dName = document.getElementById('destName').value || 'Destination';
        const avoidBlocked = document.getElementById('avoidBlockedToggle').checked;

        if (isNaN(sLat) || isNaN(sLng) || isNaN(dLat) || isNaN(dLng)) {
            alert('Please select both a valid Start and Destination point.');
            return;
        }

        const calcBtn = document.getElementById('btnCalculateRoute');
        if (calcBtn) {
            calcBtn.disabled = true;
            calcBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-2"></i>Calculating...';
        }

        const payload = {
            sourceLat: sLat,
            sourceLng: sLng,
            sourceName: sName,
            destLat: dLat,
            destLng: dLng,
            destName: dName,
            avoidBlockedRoads: avoidBlocked,
            preferLeastCongested: true
        };

        fetch('/api/routes/calculate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
        .then(res => res.json())
        .then(res => {
            if (res.success && res.data) {
                this.lastCalculationResponse = res.data;
                this.renderCalculatedRoute(res.data);
                this.renderRouteDetails(res.data);

                // Hide recalculate prompt if visible
                document.getElementById('recalculateAlert')?.classList.add('d-none');
            } else {
                alert('Route calculation failed: ' + (res.message || 'Unknown error'));
            }
        })
        .catch(err => {
            console.error('Route calculation error', err);
            alert('Unable to calculate route. Please try again.');
        })
        .finally(() => {
            if (calcBtn) {
                calcBtn.disabled = false;
                calcBtn.innerHTML = '<i class="fa-solid fa-route me-2"></i>Calculate Route';
            }
        });
    },

    renderCalculatedRoute: function (data) {
        // Clear previous polylines & hazard markers
        if (this.currentRouteLayer) this.map.removeLayer(this.currentRouteLayer);
        if (this.congestedRouteLayer) this.map.removeLayer(this.congestedRouteLayer);
        if (this.altRouteLayer) this.map.removeLayer(this.altRouteLayer);
        this.hazardMarkers.forEach(m => this.map.removeLayer(m));
        this.hazardMarkers = [];

        const coords = data.coordinates || data.routeCoordinates;
        if (!coords || coords.length === 0) return;

        // Draw primary intelligent route polyline (cyan gradient glow)
        this.currentRouteLayer = L.polyline(coords, {
            color: '#06b6d4',
            weight: 6,
            opacity: 0.85,
            lineJoin: 'round'
        }).addTo(this.map);

        // Highlight alternative detour route in Emerald Green (dashed)
        if (data.hasAlternative && data.alternativeCoordinates && data.alternativeCoordinates.length > 0) {
            this.altRouteLayer = L.polyline(data.alternativeCoordinates, {
                color: '#10b981',
                weight: 5,
                opacity: 0.8,
                dashArray: '8, 8',
                lineJoin: 'round'
            }).addTo(this.map);
            this.altRouteLayer.bindPopup(`<b><i class="fa-solid fa-shield-halved text-success me-1"></i> Alternative Detour Route</b><br>${data.alternativeSummary || 'Detour around reported traffic hazards'}<br><b>Distance:</b> ${data.alternativeDistanceKm} km &bull; <b>ETA:</b> ${data.alternativeDurationMins} mins`);
        }

        // Highlight congested sections in RED
        if (data.congestedCoordinates && data.congestedCoordinates.length > 0) {
            this.congestedRouteLayer = L.polyline(data.congestedCoordinates, {
                color: '#ef4444',
                weight: 8,
                opacity: 0.95,
                lineJoin: 'round',
                dashArray: '8, 8'
            }).addTo(this.map);
        }

        // Render markers for avoided blocked obstacles
        if (data.blockedIncidentsAvoided && data.blockedIncidentsAvoided.length > 0) {
            data.blockedIncidentsAvoided.forEach(b => {
                const icon = L.divIcon({
                    html: '<div class="custom-incident-pin" style="background:#dc2626;"><i class="fa-solid fa-ban text-white"></i></div>',
                    iconSize: [32, 32],
                    iconAnchor: [16, 16]
                });
                const m = L.marker([b.latitude, b.longitude], { icon: icon }).addTo(this.map);
                m.bindPopup(`<b class="text-danger">Hazard Avoided:</b><br>${b.category} at ${b.address || ''}`);
                this.hazardMarkers.push(m);
            });
        }

        // Render markers for congestion incidents encountered
        if (data.congestionIncidentsEncountered && data.congestionIncidentsEncountered.length > 0) {
            data.congestionIncidentsEncountered.forEach(c => {
                const icon = L.divIcon({
                    html: '<div class="custom-incident-pin" style="background:#f59e0b;"><i class="fa-solid fa-car text-white"></i></div>',
                    iconSize: [28, 28],
                    iconAnchor: [14, 14]
                });
                const m = L.marker([c.latitude, c.longitude], { icon: icon }).addTo(this.map);
                m.bindPopup(`<b class="text-warning">Traffic Alert:</b><br>${c.category} at ${c.address || ''}<br>${c.description || ''}`);
                this.hazardMarkers.push(m);
            });
        }

        // Fit map bounds to encompass the complete route
        this.map.fitBounds(this.currentRouteLayer.getBounds(), { padding: [50, 50] });
    },

    renderRouteDetails: function (data) {
        document.getElementById('routeStatsCard')?.classList.remove('d-none');
        document.getElementById('totalDistance').textContent = `${data.totalDistanceKm || data.distanceKm || 0} km`;
        document.getElementById('totalDuration').textContent = `${data.estimatedDurationMins || data.durationMinutes || 0} mins`;

        // Warning banner
        const warnEl = document.getElementById('routeWarningBanner');
        const warnText = document.getElementById('routeWarningText');
        if (warnEl && warnText) {
            if (data.warningMessage) {
                warnText.textContent = data.warningMessage;
                warnEl.classList.remove('d-none');
            } else {
                warnEl.classList.add('d-none');
            }
        }

        // Alternative Route Card
        const altCard = document.getElementById('altRouteCard');
        if (altCard) {
            if (data.hasAlternative) {
                document.getElementById('altDistance').textContent = `${data.alternativeDistanceKm} km`;
                document.getElementById('altDuration').textContent = `${data.alternativeDurationMins} mins`;
                document.getElementById('altSummary').textContent = data.alternativeSummary || 'Detour route available avoiding reported traffic hazards.';
                altCard.classList.remove('d-none');
            } else {
                altCard.classList.add('d-none');
            }
        }

        const delayNotice = document.getElementById('delayNotice');
        if (delayNotice) {
            if (data.delayMins > 0) {
                delayNotice.innerHTML = `<span class="badge bg-danger-subtle text-danger"><i class="fa-solid fa-triangle-exclamation me-1"></i>+${data.delayMins} min traffic delay</span>`;
                delayNotice.classList.remove('d-none');
            } else {
                delayNotice.innerHTML = `<span class="badge bg-success-subtle text-success"><i class="fa-solid fa-check me-1"></i>Traffic Flow Clear</span>`;
                delayNotice.classList.remove('d-none');
            }
        }

        const detourNotice = document.getElementById('detourNotice');
        if (detourNotice) {
            if (data.hasBlockedAvoidance && data.blockedIncidentsAvoided && data.blockedIncidentsAvoided.length > 0) {
                detourNotice.innerHTML = `<span class="badge bg-warning-subtle text-warning"><i class="fa-solid fa-shield-halved me-1"></i>Avoided ${data.blockedIncidentsAvoided.length} roadblock(s)</span>`;
                detourNotice.classList.remove('d-none');
            } else {
                detourNotice.classList.add('d-none');
            }
        }

        // Render Step by Step Instructions
        const stepsContainer = document.getElementById('routeStepsList');
        const steps = data.steps || data.turnByTurnSteps;
        if (stepsContainer && steps) {
            stepsContainer.innerHTML = '';
            steps.forEach((step, idx) => {
                const item = document.createElement('div');
                item.className = 'p-2 mb-2 rounded bg-surface border border-secondary-subtle small';
                item.innerHTML = `
                    <div class="d-flex justify-content-between align-items-center">
                        <span class="fw-semibold text-light"><i class="fa-solid fa-turn-up text-primary me-2"></i>${step.instruction}</span>
                        <span class="text-secondary">${step.distanceKm} km</span>
                    </div>
                    ${step.incidentWarning ? `<div class="text-danger mt-1 small"><i class="fa-solid fa-circle-exclamation me-1"></i>${step.incidentWarning}</div>` : ''}
                `;
                stepsContainer.appendChild(item);
            });
        }
    },

    saveCurrentRoute: function () {
        if (!this.lastCalculationResponse) {
            alert('Please calculate a route first.');
            return;
        }

        const payload = {
            sourceLat: parseFloat(document.getElementById('sourceLat').value),
            sourceLng: parseFloat(document.getElementById('sourceLng').value),
            sourceName: document.getElementById('sourceName').value,
            destLat: parseFloat(document.getElementById('destLat').value),
            destLng: parseFloat(document.getElementById('destLng').value),
            destName: document.getElementById('destName').value,
            avoidBlockedRoads: document.getElementById('avoidBlockedToggle').checked
        };

        fetch('/api/routes/save', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
        .then(res => res.json())
        .then(res => {
            if (res.success) {
                SmartNav.showToast({
                    title: 'Route Saved',
                    message: 'Route has been archived to your personal dashboard.',
                    type: 'success'
                });
            } else {
                alert('Failed to save route: ' + res.message);
            }
        })
        .catch(err => console.error(err));
    },

    // Simulation & Live GPS Navigation State
    vehicleMarker: null,
    simTimer: null,
    simIndex: 0,
    simCoords: [],
    simSpeed: 48,
    isSimulating: false,
    watchId: null,

    startSimulation: function () {
        if (!this.lastCalculationResponse || !this.lastCalculationResponse.routeCoordinates || this.lastCalculationResponse.routeCoordinates.length < 2) {
            SmartNav.showToast({ title: 'Navigation', message: 'Please calculate a route before starting live drive simulation.', type: 'warning' });
            return;
        }

        // Interpolate points for smooth vehicle animation
        this.simCoords = this.interpolateRoute(this.lastCalculationResponse.routeCoordinates, 25);
        this.simIndex = 0;
        this.isSimulating = true;

        document.getElementById('btnStartSimulation')?.classList.add('d-none');
        document.getElementById('btnStopSimulation')?.classList.remove('d-none');
        document.getElementById('simulationHud')?.classList.remove('d-none');

        // Create or reset animated vehicle marker
        const startPos = this.simCoords[0];
        if (!this.vehicleMarker) {
            const vehicleIcon = L.divIcon({
                html: '<div id="vehicleIconElement" class="vehicle-nav-marker"><i class="fa-solid fa-car-side"></i></div>',
                className: 'custom-vehicle-container',
                iconSize: [44, 44],
                iconAnchor: [22, 22]
            });
            this.vehicleMarker = L.marker(startPos, { icon: vehicleIcon, zIndexOffset: 2000 }).addTo(this.map);
        } else {
            this.vehicleMarker.setLatLng(startPos);
        }

        this.map.panTo(startPos, { animate: true });

        // Start step timer
        clearInterval(this.simTimer);
        this.simTimer = setInterval(() => this.stepSimulation(), 120);

        SmartNav.showToast({
            title: 'Drive Simulation Started',
            message: 'Vehicle is navigating along the dynamic route with live telemetry HUD.',
            type: 'success'
        });
    },

    stopSimulation: function () {
        this.isSimulating = false;
        clearInterval(this.simTimer);
        if (this.vehicleMarker) {
            this.map.removeLayer(this.vehicleMarker);
            this.vehicleMarker = null;
        }
        document.getElementById('btnStartSimulation')?.classList.remove('d-none');
        document.getElementById('btnStopSimulation')?.classList.add('d-none');
        document.getElementById('simulationHud')?.classList.add('d-none');
        document.getElementById('hudHazardAlert')?.classList.add('d-none');
    },

    stepSimulation: function () {
        if (!this.isSimulating || this.simIndex >= this.simCoords.length - 1) {
            this.finishSimulation();
            return;
        }

        this.simIndex++;
        const currentPos = this.simCoords[this.simIndex];
        const prevPos = this.simCoords[this.simIndex - 1];

        this.vehicleMarker.setLatLng(currentPos);

        // Keep map centered on vehicle smoothly
        if (this.simIndex % 8 === 0) {
            this.map.panTo(currentPos, { animate: true });
        }

        // Calculate bearing / vehicle heading rotation
        const bearing = this.calculateBearing(prevPos[0], prevPos[1], currentPos[0], currentPos[1]);
        const iconEl = document.getElementById('vehicleIconElement');
        if (iconEl) {
            iconEl.style.transform = `rotate(${bearing}deg)`;
        }

        // Check if currently traversing a congested zone or near hazards
        const isNearCongestion = this.isNearCongestedZone(currentPos);
        const nearHazard = this.findNearbyHazard(currentPos, 350);

        const speedEl = document.getElementById('hudSpeed');
        const alertEl = document.getElementById('hudHazardAlert');
        const alertMsg = document.getElementById('hudHazardMsg');

        if (isNearCongestion) {
            this.simSpeed = Math.floor(14 + Math.random() * 5); // 14-19 km/h in traffic
            if (speedEl) {
                speedEl.textContent = this.simSpeed;
                speedEl.className = 'hud-speed-value text-danger';
            }
            if (alertEl && alertMsg) {
                alertEl.classList.remove('d-none');
                alertMsg.textContent = 'Heavy Traffic Congestion ahead! Speed slowed to 16 km/h.';
            }
        } else {
            this.simSpeed = Math.floor(48 + Math.random() * 6); // 48-54 km/h cruising
            if (speedEl) {
                speedEl.textContent = this.simSpeed;
                speedEl.className = 'hud-speed-value text-primary';
            }
            if (nearHazard) {
                if (alertEl && alertMsg) {
                    alertEl.classList.remove('d-none');
                    alertMsg.textContent = `Hazard Radar: ${nearHazard.category} in ${nearHazard.distance}m!`;
                }
            } else {
                if (alertEl) alertEl.classList.add('d-none');
            }
        }

        // Update remaining distance and ETA
        const progressRatio = this.simIndex / this.simCoords.length;
        const totalDist = this.lastCalculationResponse.distanceKm || 2.5;
        const totalDur = this.lastCalculationResponse.durationMinutes || 8;

        const distRemaining = Math.max(0, (totalDist * (1 - progressRatio))).toFixed(1);
        const durRemaining = Math.max(1, Math.round(totalDur * (1 - progressRatio)));

        const distLeftEl = document.getElementById('hudDistanceLeft');
        const durLeftEl = document.getElementById('hudDurationLeft');
        if (distLeftEl) distLeftEl.textContent = `${distRemaining} km remaining`;
        if (durLeftEl) durLeftEl.textContent = `${durRemaining} min ETA`;

        // Update current maneuver instruction based on progress
        const steps = this.lastCalculationResponse.turnByTurnSteps || [];
        if (steps.length > 0) {
            const stepIdx = Math.min(Math.floor(progressRatio * steps.length), steps.length - 1);
            const curStep = steps[stepIdx];
            const hudInstruction = document.getElementById('hudInstruction');
            if (hudInstruction) {
                hudInstruction.innerHTML = `<i class="fa-solid fa-arrow-turn-up text-info me-1"></i> ${curStep.instruction}`;
            }
        }
    },

    finishSimulation: function () {
        clearInterval(this.simTimer);
        this.isSimulating = false;
        document.getElementById('btnStartSimulation')?.classList.remove('d-none');
        document.getElementById('btnStopSimulation')?.classList.add('d-none');
        document.getElementById('hudHazardAlert')?.classList.add('d-none');

        const hudInstruction = document.getElementById('hudInstruction');
        if (hudInstruction) {
            hudInstruction.innerHTML = '<i class="fa-solid fa-flag-checkered text-success me-1"></i> Arrived at Destination!';
        }
        const speedEl = document.getElementById('hudSpeed');
        if (speedEl) {
            speedEl.textContent = '0';
            speedEl.className = 'hud-speed-value text-secondary';
        }

        SmartNav.showToast({
            title: 'Trip Completed! 🎉',
            message: 'You have safely reached your destination bypassing all blocked roads.',
            type: 'success'
        });
    },

    interpolateRoute: function (coords, segmentsPerLeg = 20) {
        const result = [];
        for (let i = 0; i < coords.length - 1; i++) {
            const p1 = coords[i];
            const p2 = coords[i + 1];
            for (let s = 0; s < segmentsPerLeg; s++) {
                const t = s / segmentsPerLeg;
                result.push([
                    p1[0] + (p2[0] - p1[0]) * t,
                    p1[1] + (p2[1] - p1[1]) * t
                ]);
            }
        }
        result.push(coords[coords.length - 1]);
        return result;
    },

    calculateBearing: function (lat1, lon1, lat2, lon2) {
        const toRad = deg => deg * Math.PI / 180;
        const toDeg = rad => rad * 180 / Math.PI;
        const φ1 = toRad(lat1), φ2 = toRad(lat2);
        const Δλ = toRad(lon2 - lon1);
        const y = Math.sin(Δλ) * Math.cos(φ2);
        const x = Math.cos(φ1) * Math.sin(φ2) - Math.sin(φ1) * Math.cos(φ2) * Math.cos(Δλ);
        return (toDeg(Math.atan2(y, x)) + 360) % 360;
    },

    isNearCongestedZone: function (currentPos) {
        if (!this.lastCalculationResponse || !this.lastCalculationResponse.congestedCoordinates) return false;
        const congCoords = this.lastCalculationResponse.congestedCoordinates;
        for (let p of congCoords) {
            const dist = this.haversineMeters(currentPos[0], currentPos[1], p[0], p[1]);
            if (dist < 350) return true;
        }
        return false;
    },

    findNearbyHazard: function (currentPos, thresholdMeters = 350) {
        if (!this.hazardMarkers) return null;
        for (let h of this.hazardMarkers) {
            if (h.getLatLng) {
                const ll = h.getLatLng();
                const dist = Math.round(this.haversineMeters(currentPos[0], currentPos[1], ll.lat, ll.lng));
                if (dist <= thresholdMeters) {
                    return { category: 'Traffic Alert', distance: dist };
                }
            }
        }
        return null;
    },

    haversineMeters: function (lat1, lon1, lat2, lon2) {
        const R = 6371000;
        const dLat = (lat2 - lat1) * Math.PI / 180;
        const dLon = (lon2 - lon1) * Math.PI / 180;
        const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                  Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                  Math.sin(dLon / 2) * Math.sin(dLon / 2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    },

    toggleGpsDevice: function (enabled) {
        if (!enabled) {
            if (this.watchId) {
                navigator.geolocation.clearWatch(this.watchId);
                this.watchId = null;
            }
            if (this.vehicleMarker && !this.isSimulating) {
                this.map.removeLayer(this.vehicleMarker);
                this.vehicleMarker = null;
            }
            document.getElementById('simulationHud')?.classList.add('d-none');
            return;
        }

        if (!navigator.geolocation) {
            alert('Geolocation not supported on this device.');
            document.getElementById('gpsDeviceToggle').checked = false;
            return;
        }

        document.getElementById('simulationHud')?.classList.remove('d-none');

        this.watchId = navigator.geolocation.watchPosition(pos => {
            const lat = pos.coords.latitude;
            const lng = pos.coords.longitude;
            const speedKmh = pos.coords.speed ? Math.round(pos.coords.speed * 3.6) : 0;

            const speedEl = document.getElementById('hudSpeed');
            if (speedEl) speedEl.textContent = speedKmh;

            if (!this.vehicleMarker) {
                const vehicleIcon = L.divIcon({
                    html: '<div class="vehicle-nav-marker"><i class="fa-solid fa-location-arrow"></i></div>',
                    iconSize: [44, 44],
                    iconAnchor: [22, 22]
                });
                this.vehicleMarker = L.marker([lat, lng], { icon: vehicleIcon, zIndexOffset: 2000 }).addTo(this.map);
            } else {
                this.vehicleMarker.setLatLng([lat, lng]);
            }
            this.map.panTo([lat, lng], { animate: true });
        }, err => {
            console.warn('GPS watch error', err);
        }, { enableHighAccuracy: true });
    },

    bindEvents: function () {
        document.getElementById('btnCalculateRoute')?.addEventListener('click', () => this.calculateRoute());
        document.getElementById('btnSaveRoute')?.addEventListener('click', () => this.saveCurrentRoute());
        document.getElementById('btnRecalculate')?.addEventListener('click', () => this.calculateRoute());

        document.getElementById('btnStartSimulation')?.addEventListener('click', () => this.startSimulation());
        document.getElementById('btnStopSimulation')?.addEventListener('click', () => this.stopSimulation());
        document.getElementById('btnHudClose')?.addEventListener('click', () => {
            document.getElementById('simulationHud')?.classList.add('d-none');
        });

        document.getElementById('gpsDeviceToggle')?.addEventListener('change', (e) => {
            this.toggleGpsDevice(e.target.checked);
        });

        document.getElementById('pickSourceBtn')?.addEventListener('click', (e) => {
            this.pickingTarget = 'source';
            e.currentTarget.classList.add('active');
            SmartNav.showToast({ title: 'Pick on Map', message: 'Click anywhere on the map to set Start point', type: 'info' });
        });

        document.getElementById('pickDestBtn')?.addEventListener('click', (e) => {
            this.pickingTarget = 'dest';
            e.currentTarget.classList.add('active');
            SmartNav.showToast({ title: 'Pick on Map', message: 'Click anywhere on the map to set Destination', type: 'info' });
        });

        document.getElementById('presetSelect')?.addEventListener('change', (e) => {
            const val = e.target.value;
            if (this.presets[val]) {
                this.applyPreset(val);
                this.calculateRoute();
            }
        });
    }
};

window.RouteEngine = RouteEngine;

