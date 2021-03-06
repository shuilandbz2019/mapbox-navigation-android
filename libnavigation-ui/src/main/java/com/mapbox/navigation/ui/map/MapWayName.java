package com.mapbox.navigation.ui.map;

import android.graphics.PointF;
import android.location.Location;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.navigation.core.MapboxNavigation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.mapbox.navigation.ui.map.NavigationMapboxMap.STREETS_LAYER_ID;

class MapWayName {

  private static final String NAME_PROPERTY = "name";
  private static final String EMPTY_CURRENT_WAY_NAME = "";
  private final MapWayNameChangeObserver mapWayNameChangeObserver = new MapWayNameChangeObserver(this);
  @NonNull
  private final Set<OnWayNameChangedListener> onWayNameChangedListeners;
  private WaynameFeatureFinder featureInteractor;
  @Nullable
  private List<Point> currentStepPoints = new ArrayList<>();
  @Nullable
  private Location currentLocation = null;
  private MapboxNavigation navigation;
  private boolean isAutoQueryEnabled;
  @Nullable
  private FeatureFilterTask filterTask;
  private String wayName = EMPTY_CURRENT_WAY_NAME;

  MapWayName(WaynameFeatureFinder featureInteractor, @NonNull MapPaddingAdjustor paddingAdjustor) {
    this.featureInteractor = featureInteractor;
    paddingAdjustor.updatePaddingWithDefault();
    this.onWayNameChangedListeners = new HashSet<>();
  }

  void updateWayNameWithPoint(PointF point) {
    if (!isAutoQueryEnabled) {
      return;
    }
    List<Feature> roadLabelFeatures = findRoadLabelFeatures(point);
    boolean invalidLabelFeatures = roadLabelFeatures.isEmpty();
    if (invalidLabelFeatures) {
      return;
    }
    executeFeatureFilterTask(roadLabelFeatures);
  }

  void updateProgress(@Nullable List<Point> currentStepPoints) {
    if (currentStepPoints != null && !this.currentStepPoints.equals(currentStepPoints)) {
      this.currentStepPoints = currentStepPoints;
    }
  }

  void updateLocation(Location currentLocation) {
    if (this.currentLocation == null || !this.currentLocation.equals(currentLocation)) {
      this.currentLocation = currentLocation;
    }
  }

  void updateWayNameQueryMap(boolean isEnabled) {
    isAutoQueryEnabled = isEnabled;
  }

  void addProgressChangeListener(MapboxNavigation navigation) {
    this.navigation = navigation;
    registerObservers();
  }

  void removeProgressChangeListener() {
    unregisterObservers();
  }

  boolean addOnWayNameChangedListener(OnWayNameChangedListener listener) {
    return onWayNameChangedListeners.add(listener);
  }

  boolean removeOnWayNameChangedListener(OnWayNameChangedListener listener) {
    return onWayNameChangedListeners.remove(listener);
  }

  void onStart() {
    registerObservers();
  }

  void onStop() {
    if (isTaskRunning()) {
      filterTask.cancel(true);
    }
    unregisterObservers();
  }

  private void registerObservers() {
    if (navigation != null) {
      navigation.registerRouteProgressObserver(mapWayNameChangeObserver);
      navigation.registerLocationObserver(mapWayNameChangeObserver);
    }
  }

  private void unregisterObservers() {
    if (navigation != null) {
      navigation.unregisterRouteProgressObserver(mapWayNameChangeObserver);
      navigation.unregisterLocationObserver(mapWayNameChangeObserver);
    }
  }

  private List<Feature> findRoadLabelFeatures(PointF point) {
    String[] layerIds = {STREETS_LAYER_ID};
    return featureInteractor.queryRenderedFeatures(point, layerIds);
  }

  private void executeFeatureFilterTask(List<Feature> roadFeatures) {
    if (isTaskRunning()) {
      filterTask.cancel(true);
    }

    if (hasValidProgressData()) {
      filterTask = new FeatureFilterTask(roadFeatures, currentLocation, currentStepPoints,
        new OnFeatureFilteredCallback() {
          @Override
          public void onFeatureFiltered(@NonNull Feature feature) {
            updateWayNameLayerWithNameProperty(feature);
          }
        });
      filterTask.execute();
    }
  }

  private boolean isTaskRunning() {
    return filterTask != null
      && (filterTask.getStatus() == AsyncTask.Status.PENDING
      || filterTask.getStatus() == AsyncTask.Status.RUNNING);
  }

  private boolean hasValidProgressData() {
    return currentLocation != null && !currentStepPoints.isEmpty();
  }

  private void updateWayNameLayerWithNameProperty(@NonNull Feature roadFeature) {
    boolean hasValidNameProperty = roadFeature.hasNonNullValueForProperty(NAME_PROPERTY);
    if (hasValidNameProperty) {
      String currentWayName = roadFeature.getStringProperty(NAME_PROPERTY);
      boolean newWayName = !wayName.contentEquals(currentWayName);
      if (newWayName) {
        updateListenersWith(currentWayName);
        wayName = currentWayName;
      }
    } else {
      updateListenersWith(EMPTY_CURRENT_WAY_NAME);
    }
  }

  private void updateListenersWith(@NonNull String currentWayName) {
    for (OnWayNameChangedListener listener : onWayNameChangedListeners) {
      listener.onWayNameChanged(currentWayName);
    }
  }
}
