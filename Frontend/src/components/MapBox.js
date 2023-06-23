import React, { useEffect, useRef } from 'react';
import mapboxgl from 'mapbox-gl';

mapboxgl.accessToken =
  'pk.eyJ1IjoiZGF2ZWRveWxlIiwiYSI6ImNsajVnNm0xYzA5a3ozZXBlYzJmY2FldWIifQ.flqjNTDCZ5tNntgbrBtB1A';

const MapBox = () => {
  const mapContainerRef = useRef(null);

  useEffect(() => {
    const map = new mapboxgl.Map({
      container: mapContainerRef.current,
      style: 'mapbox://styles/mapbox/dark-v11',
      center: [-73.978285, 40.755091],
      zoom: 13,
    });

    map.on('load', () => {
      // Add a GeoJSON source
      map.addSource('zones', {
        type: 'geojson',
        data: '/manhattan_zones.geojson',
        generateId: true,
      });

      map.addLayer({
        id: 'zone',
        type: 'fill',
        source: 'zones',
        layout: {},
        paint: {
          'fill-color': [
            'case',
            ['boolean', ['feature-state', 'hover'], false],
            'red', // Change fill color when hovering
            'blue', // Default fill color
          ],
          'fill-opacity': 0.5,
          'fill-outline-color': 'red',
          // 'fill-translate': [
          //   'case',
          //   ['boolean', ['feature-state', 'hover'], false],
          //   ['literal', [10, 10]], // Translate when hovering
          //   ['literal', [0, 0]], // Default translation
          // ],
        },
      });

      map.on('mousemove', 'zone', (e) => {
        const { objectid } = e.features[0].properties;
        console.log('Hovered Zone ID:', objectid);

        // Reset previously hovered zone to blue
        map.querySourceFeatures('zones').forEach((feature) => {
          map.setFeatureState({ source: 'zones', id: feature.id }, { hover: false });
        });

        // Set currently hovered zone to red
        e.features.forEach((feature) => {
          if (feature.properties.objectid === objectid) {
            map.setFeatureState({ source: 'zones', id: feature.id }, { hover: true });
          }
        });

        map.getCanvas().style.cursor = 'pointer';
      });

      map.on('mouseleave', 'zone', () => {
        map.querySourceFeatures('zones').forEach((feature) => {
          map.setFeatureState({ source: 'zones', id: feature.id }, { hover: false });
        });

        map.getCanvas().style.cursor = '';
      });

      // Add controls (optional)
      map.addControl(new mapboxgl.NavigationControl());
    });

    // Clean up
    return () => map.remove();
  }, []);

  return <div ref={mapContainerRef} style={{ width: '100%', height: '600px' }} />;
};

export default MapBox;
