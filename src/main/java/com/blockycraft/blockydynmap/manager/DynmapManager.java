package com.blockycraft.blockydynmap.manager;

import com.blockycraft.blockyclaim.data.Claim;
import com.blockycraft.blockydynmap.BlockyDynmap;
import com.blockycraft.blockygroups.data.Group;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;
import org.bukkit.plugin.Plugin;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DynmapManager {
    private static final String MARKER_SET_ID = "blockyclaim.markers";
    private static final String MARKER_SET_LABEL = "Territórios (BlockyClaim)";
    private static final String DEFAULT_COLOR = "#808080";
    private static final double FILL_OPACITY = 0.3;
    private static final double LINE_OPACITY = 0.8;
    private static final int LINE_WEIGHT = 3;
    private final BlockyDynmap plugin;
    private MarkerAPI markerApi;
    private MarkerSet markerSet;

    public DynmapManager(BlockyDynmap plugin) {
        this.plugin = plugin;
        setupDynmap();
    }

    private void setupDynmap() {
        Plugin dynmapPlugin = plugin.getServer().getPluginManager().getPlugin("dynmap");
        if (dynmapPlugin instanceof DynmapAPI) {
            DynmapAPI api = (DynmapAPI) dynmapPlugin;
            this.markerApi = api.getMarkerAPI();
            this.markerSet = markerApi.getMarkerSet(MARKER_SET_ID);
            if (this.markerSet == null) {
                this.markerSet = markerApi.createMarkerSet(MARKER_SET_ID, MARKER_SET_LABEL, null, false);
            } else {
                this.markerSet.setMarkerSetLabel(MARKER_SET_LABEL);
            }
        }
    }

    public void syncMarkers(List<Claim> claims) {
        Map<String, List<Claim>> claimsByOwner = new HashMap<>();
        for (Claim claim : claims) {
            String owner = claim.getOwnerName();
            claimsByOwner.computeIfAbsent(owner, k -> new ArrayList<>()).add(claim);
        }

        Set<String> currentMarkerIds = new HashSet<>();

        for (Map.Entry<String, List<Claim>> entry : claimsByOwner.entrySet()) {
            List<Claim> ownerClaims = entry.getValue();
            if (ownerClaims.isEmpty()) continue;

            Area mergedArea = mergeClaims(ownerClaims);

            Claim representativeClaim = ownerClaims.get(0);
            String markerId = generateMarkerId(representativeClaim.getOwnerName());

            currentMarkerIds.add(markerId);
            createOrUpdateClaimMarker(representativeClaim, mergedArea, markerId);
        }

        for (AreaMarker marker : markerSet.getAreaMarkers()) {
            if (!currentMarkerIds.contains(marker.getMarkerID().replaceAll("_[0-9]+$", ""))) {
                marker.deleteMarker();
            }
        }
    }

    public void createOrUpdateClaimMarker(Claim claim, Area area, String markerId) {
        if (markerSet == null || claim == null) return;
        String worldName = claim.getWorldName();
        // Remove existing markers for this owner
        for (AreaMarker marker : markerSet.getAreaMarkers()) {
            if (marker.getMarkerID().startsWith(markerId)) {
                marker.deleteMarker();
            }
        }

        List<double[][]> polygons = convertAreaToPolygons(area);

        for (int i = 0; i < polygons.size(); i++) {
            double[] xCorners = polygons.get(i)[0];
            double[] zCorners = polygons.get(i)[1];
            AreaMarker marker = markerSet.createAreaMarker(markerId + "_" + i, "", false, worldName, xCorners, zCorners, false);
            if (marker == null) {
                System.out.println("[BlockyDynmap] Erro: Nao foi possivel criar o marcador para o claim: " + claim.getClaimName());
                return;
            }
            String ownerName = claim.getOwnerName();
            Group ownerGroup = plugin.getBlockyGroups().getGroupManager().getPlayerGroup(ownerName);
            String color, label;
            if (ownerGroup != null) {
                color = ownerGroup.getColorHex();
                label = "§f" + ownerGroup.getTag();
            } else {
                color = DEFAULT_COLOR;
                label = "§f" + ownerName;
            }
            StringBuilder description = new StringBuilder();
            description.append("<b>Dono:</b> ").append(ownerName);
            if (ownerGroup != null) {
                description.append("<br/><b>Grupo:</b> ").append(ownerGroup.getName());
            }
            marker.setLabel(label, true);
            marker.setDescription(description.toString());
            int colorInt = Integer.parseInt(color.substring(1), 16);
            marker.setLineStyle(LINE_WEIGHT, LINE_OPACITY, colorInt);
            marker.setFillStyle(FILL_OPACITY, colorInt);
        }
    }

    private Area mergeClaims(List<Claim> claims) {
        Area mergedArea = new Area();
        for (Claim claim : claims) {
            Rectangle rect = new Rectangle(claim.getMinX(), claim.getMinZ(), claim.getMaxX() - claim.getMinX() + 1, claim.getMaxZ() - claim.getMinZ() + 1);
            mergedArea.add(new Area(rect));
        }
        return mergedArea;
    }

    private List<double[][]> convertAreaToPolygons(Area area) {
        List<double[][]> polygons = new ArrayList<>();
        PathIterator pathIterator = area.getPathIterator(null);
        List<Double> currentX = new ArrayList<>();
        List<Double> currentZ = new ArrayList<>();
        double[] coords = new double[6];

        while (!pathIterator.isDone()) {
            int type = pathIterator.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    currentX.clear();
                    currentZ.clear();
                    currentX.add(coords[0]);
                    currentZ.add(coords[1]);
                    break;
                case PathIterator.SEG_LINETO:
                    currentX.add(coords[0]);
                    currentZ.add(coords[1]);
                    break;
                case PathIterator.SEG_CLOSE:
                    double[] xArray = new double[currentX.size()];
                    double[] zArray = new double[currentZ.size()];
                    for (int i = 0; i < currentX.size(); i++) {
                        xArray[i] = currentX.get(i);
                        zArray[i] = currentZ.get(i);
                    }
                    polygons.add(new double[][]{xArray, zArray});
                    break;
            }
            pathIterator.next();
        }
        return polygons;
    }

    public void cleanup() {
        // Limpeza customizada se necessário
    }

    private String generateMarkerId(String ownerName) {
        return "claim_" + ownerName.toLowerCase(Locale.ROOT);
    }
}