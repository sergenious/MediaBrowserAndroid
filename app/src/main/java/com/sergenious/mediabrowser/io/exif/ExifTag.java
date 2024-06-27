package com.sergenious.mediabrowser.io.exif;

import android.content.Context;

import com.sergenious.mediabrowser.R;

import java.util.HashMap;
import java.util.Map;

public enum ExifTag {
    UNKNOWN(ExifIfdType.STANDARD, 0, 0, null, null, null),
    INTEROP_INDEX(ExifIfdType.STANDARD, 0x0001, R.string.interop_index, null, null, null),
    INTEROP_VERSION(ExifIfdType.STANDARD, 0x0002, R.string.interop_version, null, null, null),
    PROCESSING_SOFTWARE(ExifIfdType.STANDARD, 0x000b, R.string.processing_software, null, null, null),
    SUBFILE_TYPE(ExifIfdType.STANDARD, 0x00fe, R.string.subfile_type, null, null, ExifLabels.SUBFILE_TYPE_LABEL_IDS),
    IMAGE_WIDTH(ExifIfdType.STANDARD, 0x0100, R.string.image_width, null, null, null),
    IMAGE_HEIGHT(ExifIfdType.STANDARD, 0x0101, R.string.image_height, null, null, null),
    BITS_PER_SAMPLE(ExifIfdType.STANDARD, 0x0102, R.string.bits_per_sample, null, null, null),
    COMPRESSION(ExifIfdType.STANDARD, 0x0103, R.string.compression, null, null, ExifLabels.COMPRESSION_LABEL_IDS),
    PHOTOMETRIC_INTERPRETATION(ExifIfdType.STANDARD, 0x0106, R.string.photometric_interpretation, null, null, ExifLabels.PHOTOMETRIC_INTERPRETATION_LABEL_IDS),
    THRESHOLDING(ExifIfdType.STANDARD, 0x0107, R.string.thresholding, null, null, ExifLabels.THRESHOLDING_LABEL_IDS),
    CELL_WIDTH(ExifIfdType.STANDARD, 0x0108, R.string.cell_width, null, null, null),
    CELL_HEIGHT(ExifIfdType.STANDARD, 0x0109, R.string.cell_height, null, null, null),
    FILL_ORDER(ExifIfdType.STANDARD, 0x010a, R.string.fill_order, null, null, ExifLabels.FILL_ORDER_LABEL_IDS),
    DOCUMENT_NAME(ExifIfdType.STANDARD, 0x010d, R.string.document_name, null, null, null),
    IMAGE_DESCRIPTION(ExifIfdType.STANDARD, 0x010e, R.string.image_description, null, null, null),
    MAKE(ExifIfdType.STANDARD, 0x010f, R.string.make, null, null, null),
    MODEL(ExifIfdType.STANDARD, 0x0110, R.string.model, null, null, null),
    ORIENTATION(ExifIfdType.STANDARD, 0x0112, R.string.orientation, null, null, ExifLabels.ORIENTATION_LABEL_IDS),
    SAMPLES_PER_PIXEL(ExifIfdType.STANDARD, 0x0115, R.string.samples_per_pixel, null, null, null),
    MIN_SAMPLE_VALUE(ExifIfdType.STANDARD, 0x0118, R.string.min_sample_value, null, null, null),
    MAX_SAMPLE_VALUE(ExifIfdType.STANDARD, 0x0119, R.string.max_sample_value, null, null, null),
    X_RESOLUTION(ExifIfdType.STANDARD, 0x011a, R.string.x_resolution, null, null, null),
    Y_RESOLUTION(ExifIfdType.STANDARD, 0x011b, R.string.y_resolution, null, null, null),
    PLANAR_CONFIGURATION(ExifIfdType.STANDARD, 0x011c, R.string.planar_configuration, null, null, ExifLabels.PLANAR_CONFIGURATION_LABEL_IDS),
    PAGE_NAME(ExifIfdType.STANDARD, 0x011d, R.string.page_name, null, null, null),
    X_POSITION(ExifIfdType.STANDARD, 0x011e, R.string.x_position, null, null, null),
    Y_POSITION(ExifIfdType.STANDARD, 0x011f, R.string.y_position, null, null, null),
    FREE_OFFSETS(ExifIfdType.STANDARD, 0x0120, R.string.free_offsets, null, null, null),
    FREE_BYTE_COUNTS(ExifIfdType.STANDARD, 0x0121, R.string.free_byte_counts, null, null, null),
    GRAY_RESPONSE_UNIT(ExifIfdType.STANDARD, 0x0122, R.string.gray_response_unit, null, null, ExifLabels.GRAY_RESPONSE_UNIT_LABEL_IDS),
    GRAY_RESPONSE_CURVE(ExifIfdType.STANDARD, 0x0123, R.string.gray_response_curve, null, null, null),
    RESOLUTION_UNIT(ExifIfdType.STANDARD, 0x0128, R.string.resolution_unit, null, null, ExifLabels.RESOLUTION_UNIT_LABEL_IDS),
    PAGE_NUMBER(ExifIfdType.STANDARD, 0x0129, R.string.page_number, null, null, null),
    COLOR_RESPONSE_UNIT(ExifIfdType.STANDARD, 0x012c, R.string.color_response_unit, null, null, null),
    SOFTWARE(ExifIfdType.STANDARD, 0x0131, R.string.software, null, null, null),
    DATETIME(ExifIfdType.STANDARD, 0x0132, R.string.datetime, null, null, null),
    ARTIST(ExifIfdType.STANDARD, 0x013b, R.string.artist, null, null, null),
    HOST_COMPUTER(ExifIfdType.STANDARD, 0x013c, R.string.host_computer, null, null, null),
    WHITE_POINT(ExifIfdType.STANDARD, 0x013e, R.string.white_point, null, null, null),
    PRIMARY_CHROMATICITIES(ExifIfdType.STANDARD, 0x013f, R.string.primary_chromaticities, null, null, null),
    HALFTONE_HINTS(ExifIfdType.STANDARD, 0x0141, R.string.halftone_hints, null, null, null),
    CODING_METHODS(ExifIfdType.STANDARD, 0x0193, R.string.coding_methods, null, null, ExifLabels.CODING_METHODS_LABEL_IDS, true),
    VERSION_YEAR(ExifIfdType.STANDARD, 0x0194, R.string.version_year, null, null, null),
    MODE_NUMBER(ExifIfdType.STANDARD, 0x0195, R.string.mode_number, null, null, null),
    Y_CB_CR_COEFFICIENTS(ExifIfdType.STANDARD, 0x0211, R.string.y_cb_cr_coefficients, null, null, null),
    Y_CB_CR_SUBSAMPLING(ExifIfdType.STANDARD, 0x0212, R.string.y_cb_cr_subsampling, null, null, null),
    Y_CB_CR_POSITIONING(ExifIfdType.STANDARD, 0x0213, R.string.y_cb_cr_positioning, null, null, ExifLabels.Y_CB_CR_POSITIONING_LABEL_IDS),
    REFERENCE_BLACK_WHITE(ExifIfdType.STANDARD, 0x0214, R.string.reference_black_white, null, null, null),
    RELATED_IMAGE_FILE_FORMAT(ExifIfdType.STANDARD, 0x1000, R.string.related_image_file_format, null, null, null),
    RELATED_IMAGE_WIDTH(ExifIfdType.STANDARD, 0x1001, R.string.related_image_width, null, null, null),
    RELATED_IMAGE_HEIGHT(ExifIfdType.STANDARD, 0x1002, R.string.related_image_height, null, null, null),
    COPYRIGHT(ExifIfdType.STANDARD, 0x8298, R.string.copyright, null, null, null),
    EXPOSURE_TIME(ExifIfdType.STANDARD, 0x829a, R.string.exposure_time, null, " s", null),
    F_NUMBER(ExifIfdType.STANDARD, 0x829d, R.string.f_number, "F ", null, null),
    EXIF_OFFSET(ExifIfdType.STANDARD, 0x8769, 0, null, null, null),
    EXPOSURE_PROGRAM(ExifIfdType.STANDARD, 0x8822, R.string.exposure_program, null, null, ExifLabels.EXPOSURE_PROGRAM_LABEL_IDS),
    SPECTRAL_SENSITIVITY(ExifIfdType.STANDARD, 0x8824, R.string.spectral_sensitivity, null, null, null),
    EXIF_GPS(ExifIfdType.STANDARD, 0x8825, 0, null, null, null),
    ISO(ExifIfdType.STANDARD, 0x8827, R.string.iso, null, null, null),
    TIMEZONE_OFFSET(ExifIfdType.STANDARD, 0x882a, R.string.timezone_offset, null, null, null),
    SELF_TIMER_MODE(ExifIfdType.STANDARD, 0x882b, R.string.self_timer_mode, null, null, null),
    SENSITIVITY_TYPE(ExifIfdType.STANDARD, 0x8830, R.string.sensitivity_type, null, null, ExifLabels.SENSIVITY_TYPE_LABEL_IDS),
    STANDARD_OUTPUT_SENSITIVITY(ExifIfdType.STANDARD, 0x8831, R.string.standard_output_sensitivity, null, null, null),
    RECOMMENDED_EXPOSURE_INDEX(ExifIfdType.STANDARD, 0x8832, R.string.recommended_exposure_index, null, null, null),
    ISO_SPEED(ExifIfdType.STANDARD, 0x8833, R.string.iso_speed, null, null, null),
    EXIF_VERSION(ExifIfdType.STANDARD, 0x9000, R.string.exif_version, null, null, null),
    DATETIME_ORIGINAL(ExifIfdType.STANDARD, 0x9003, R.string.datetime_original, null, null, null),
    DATETIME_DIGITIZED(ExifIfdType.STANDARD, 0x9004, R.string.datetime_digitized, null, null, null),
    OFFSET_TIME(ExifIfdType.STANDARD, 0x9010, R.string.offset_time, null, null, null),
    OFFSET_TIME_ORIGINAL(ExifIfdType.STANDARD, 0x9011, R.string.offset_time_original, null, null, null),
    OFFSET_TIME_DIGITIZED(ExifIfdType.STANDARD, 0x9012, R.string.offset_time_digitized, null, null, null),
    COMPONENTS_CONFIGURATION(ExifIfdType.STANDARD, 0x9101, R.string.components_configuration, null, null, ExifLabels.COMPONENT_CONFIGURATION_LABELS),
    COMPRESSED_BITS_PER_PIXEL(ExifIfdType.STANDARD, 0x9102, R.string.compressed_bits_per_pixel, null, null, null),
    SHUTTER_SPEED(ExifIfdType.STANDARD, 0x9201, R.string.shutter_speed, null, " s", null),
    APERTURE(ExifIfdType.STANDARD, 0x9202, R.string.aperture, "F ", null, null),
    BRIGHTNESS(ExifIfdType.STANDARD, 0x9203, R.string.brightness, null, null, null),
    EXPOSURE_COMPENSATION(ExifIfdType.STANDARD, 0x9204, R.string.exposure_compensation, null, null, null),
    MAX_APERTURE(ExifIfdType.STANDARD, 0x9205, R.string.max_aperture, "F ", null, null),
    SUBJECT_DISTANCE(ExifIfdType.STANDARD, 0x9206, R.string.subject_distance, null, " m", null),
    METERING_MODE(ExifIfdType.STANDARD, 0x9207, R.string.metering_mode, null, null, ExifLabels.METERING_MODE_LABEL_IDS),
    LIGHT_SOURCE(ExifIfdType.STANDARD, 0x9208, R.string.light_source, null, null, ExifLabels.LIGHT_SOURCE_LABEL_IDS),
    FLASH(ExifIfdType.STANDARD, 0x9209, R.string.flash, null, null, ExifLabels.FLASH_LABEL_IDS),
    FOCAL_LENGTH(ExifIfdType.STANDARD, 0x920a, R.string.focal_length, null, " mm", null),
    FLASH_ENERGY(ExifIfdType.STANDARD, 0x920b, R.string.flash_energy, null, null, null),
    SPATIAL_FREQ_RESPONSE(ExifIfdType.STANDARD, 0x920c, R.string.spatial_frequency_response, null, null, null),
    NOISE(ExifIfdType.STANDARD, 0x920d, R.string.noise, null, null, null),
    FOCAL_PLANE_X_RESOLUTION(ExifIfdType.STANDARD, 0x920e, R.string.focal_plane_xres, null, null, null),
    FOCAL_PLANE_Y_RESOLUTION(ExifIfdType.STANDARD, 0x920f, R.string.focal_plane_yres, null, null, null),
    FOCAL_PLANE_RESOLUTION_UNIT(ExifIfdType.STANDARD, 0x9210, R.string.focal_plane_res_unit, null, null, ExifLabels.FOCAL_PLANE_RESOLUTION_UNIT_LABEL_IDS),
    IMAGE_NUMBER(ExifIfdType.STANDARD, 0x9211, R.string.image_number, null, null, null),
    SECURITY_CLASSIFICATION(ExifIfdType.STANDARD, 0x9212, R.string.security_classification, null, null, ExifLabels.SECURITY_CLASSIFICATION_LABEL_IDS),
    IMAGE_HISTORY(ExifIfdType.STANDARD, 0x9213, R.string.image_history, null, null, null),
    SUBJECT_AREA(ExifIfdType.STANDARD, 0x9214, R.string.subject_area, null, null, null),
    EXPOSURE_INDEX(ExifIfdType.STANDARD, 0x9215, R.string.exposure_index, null, null, null),
    SENSING_METHOD(ExifIfdType.STANDARD, 0x9217, R.string.sensing_method, null, null, ExifLabels.SENSING_MODE_LABEL_IDS),
    MAKER_NOTE(ExifIfdType.STANDARD, 0x927c, 0, null, null, null),
    USER_COMMENT(ExifIfdType.STANDARD, 0x9286, R.string.user_comment, null, null, null),
    SUBSEC_TIME(ExifIfdType.STANDARD, 0x9290, R.string.subsec_time, null, null, null),
    SUBSEC_TIME_ORIGINAL(ExifIfdType.STANDARD, 0x9291, R.string.subsec_time_original, null, null, null),
    SUBSEC_TIME_DIGITIZED(ExifIfdType.STANDARD, 0x9292, R.string.subsec_time_digitized, null, null, null),
    AMBIENT_TEMPERATURE(ExifIfdType.STANDARD, 0x9400, R.string.ambient_temperature, null, "\u00B0C", null),
    HUMIDITY(ExifIfdType.STANDARD, 0x9401, R.string.humidity, null, "%", null),
    PRESSURE(ExifIfdType.STANDARD, 0x9402, R.string.air_pressure, null, " hPa", null),
    WATER_DEPTH(ExifIfdType.STANDARD, 0x9403, R.string.water_depth, null, " m", null),
    ACCELERATION(ExifIfdType.STANDARD, 0x9404, R.string.acceleration, null, " * 10\u207B\u2075 m/s\u00B2", null),
    CAMERA_ELEVATION_ANGLE(ExifIfdType.STANDARD, 0x9405, R.string.camera_elevation_angle, null, " \u00B0", null),
    FLASHPIX_VERSION(ExifIfdType.STANDARD, 0xa000, R.string.flashpix_version, null, null, null),
    COLOR_SPACE(ExifIfdType.STANDARD, 0xa001, R.string.color_space, null, null, ExifLabels.COLOR_SPACE_LABEL_IDS),
    EXIF_IMAGE_WIDTH(ExifIfdType.STANDARD, 0xa002, R.string.exif_image_width, null, null, null),
    EXIF_IMAGE_HEIGHT(ExifIfdType.STANDARD, 0xa003, R.string.exif_image_height, null, null, null),
    RELATED_SOUND_FILE(ExifIfdType.STANDARD, 0xa004, R.string.related_sound_file, null, null, null),
    INTEROP_OFFSET(ExifIfdType.STANDARD, 0xa005, 0, null, null, null),
    FLASH_ENERGY2(ExifIfdType.STANDARD, 0xa20b, R.string.flash_energy, null, null, null),
    SPATIAL_FREQ_RESPONSE2(ExifIfdType.STANDARD, 0xa20c, R.string.spatial_frequency_response, null, null, null),
    NOISE2(ExifIfdType.STANDARD, 0xa20d, R.string.noise, null, null, null),
    FOCAL_PLANE_X_RESOLUTION2(ExifIfdType.STANDARD, 0xa20e, R.string.focal_plane_xres, null, null, null),
    FOCAL_PLANE_Y_RESOLUTION2(ExifIfdType.STANDARD, 0xa20f, R.string.focal_plane_yres, null, null, null),
    FOCAL_PLANE_RESOLUTION_UNIT2(ExifIfdType.STANDARD, 0xa210, R.string.focal_plane_res_unit, null, null, ExifLabels.FOCAL_PLANE_RESOLUTION_UNIT_LABEL_IDS),
    IMAGE_NUMBER2(ExifIfdType.STANDARD, 0xa211, R.string.image_number, null, null, null),
    SECURITY_CLASSIFICATION2(ExifIfdType.STANDARD, 0xa212, R.string.security_classification, null, null, ExifLabels.SECURITY_CLASSIFICATION_LABEL_IDS),
    IMAGE_HISTORY2(ExifIfdType.STANDARD, 0xa213, R.string.image_history, null, null, null),
    SUBJECT_LOCATION(ExifIfdType.STANDARD, 0xa214, R.string.subject_location, null, null, null),
    EXPOSURE_INDEX2(ExifIfdType.STANDARD, 0xa215, R.string.exposure_index, null, null, null),
    SENSING_METHOD2(ExifIfdType.STANDARD, 0xa217, R.string.sensing_method, null, null, ExifLabels.SENSING_MODE_LABEL_IDS),
    FILE_SOURCE(ExifIfdType.STANDARD, 0xa300, R.string.file_source, null, null, ExifLabels.FILE_SOURCE_LABEL_IDS),
    SCENE_TYPE(ExifIfdType.STANDARD, 0xa301, R.string.scene_type, null, null, ExifLabels.SCENE_TYPE_LABEL_IDS),
    CFA_PATTERN(ExifIfdType.STANDARD, 0xa302, R.string.cfa_pattern, null, null, null),
    CUSTOM_RENDERED(ExifIfdType.STANDARD, 0xa401, R.string.custom_rendered, null, null, ExifLabels.CUSTOM_RENDERED_LABEL_IDS),
    EXPOSURE_MODE(ExifIfdType.STANDARD, 0xa402, R.string.exposure_mode, null, null, ExifLabels.EXPOSURE_MODE_LABEL_IDS),
    WHITE_BALANCE(ExifIfdType.STANDARD, 0xa403, R.string.white_balance, null, null, ExifLabels.WHITE_BALANCE_LABEL_IDS),
    DIGITAL_ZOOM_RATIO(ExifIfdType.STANDARD, 0xa404, R.string.digital_zoom_ratio, null, null, null),
    FOCAL_LENGTH_IN_35MM_FORMAT(ExifIfdType.STANDARD, 0xa405, R.string.focal_length_in_35mm, null, " mm", null),
    SCENE_CAPTURE_TYPE(ExifIfdType.STANDARD, 0xa406, R.string.scene_capture_type, null, null, ExifLabels.SCENE_CAPTURE_TYPE_LABEL_IDS),
    GAIN_CONTROL(ExifIfdType.STANDARD, 0xa407, R.string.gain_control, null, null, ExifLabels.GAIN_CONTROL_LABEL_IDS),
    CONTRAST(ExifIfdType.STANDARD, 0xa408, R.string.contrast, null, null, ExifLabels.CONTRAST_LABEL_IDS),
    SATURATION(ExifIfdType.STANDARD, 0xa409, R.string.saturation, null, null, ExifLabels.SATURATION_LABEL_IDS),
    SHARPNESS(ExifIfdType.STANDARD, 0xa40a, R.string.sharpness, null, null, ExifLabels.SHARPNESS_LABEL_IDS),
    DEVICE_SETTINGS_DESCRIPTION(ExifIfdType.STANDARD, 0xa40b, R.string.device_settings_description, null, null, null),
    SUBJECT_DISTANCE_RANGE(ExifIfdType.STANDARD, 0xa40c, R.string.subject_distance_range, null, null, ExifLabels.SUBJECT_DISTANCE_RANGE_LABEL_IDS),
    IMAGE_UNIQUE_ID(ExifIfdType.STANDARD, 0xa420, R.string.image_unique_id, null, null, null),
    OWNER_NAME(ExifIfdType.STANDARD, 0xa430, R.string.owner_name, null, null, null),
    SERIAL_NUMBER(ExifIfdType.STANDARD, 0xa431, R.string.serial_number, null, null, null),
    LENS_INFO(ExifIfdType.STANDARD, 0xa432, R.string.lens_info, null, null, null),
    LENS_MAKE(ExifIfdType.STANDARD, 0xa433, R.string.lens_make, null, null, null),
    LENS_MODEL(ExifIfdType.STANDARD, 0xa434, R.string.lens_model, null, null, null),
    LENS_SERIAL_NUMBER(ExifIfdType.STANDARD, 0xa435, R.string.lens_serial_number, null, null, null),
    TITLE(ExifIfdType.STANDARD, 0xa436, R.string.title, null, null, null),
    PHOTOGRAPHER(ExifIfdType.STANDARD, 0xa437, R.string.photographer, null, null, null),
    IMAGE_EDITOR(ExifIfdType.STANDARD, 0xa438, R.string.image_editor, null, null, null),
    CAMERA_FIRMWARE(ExifIfdType.STANDARD, 0xa439, R.string.camera_firmware, null, null, null),
    GAMMA(ExifIfdType.STANDARD, 0xa500, R.string.gamma, null, null, null),

    GPS_VERSION_ID(ExifIfdType.GPS, 0x0000, 0, null, null, null),
    GPS_LATITUDE_REF(ExifIfdType.GPS, 0x0001, R.string.gps_latitude_hemisphere, null, null, ExifLabels.GPS_LATITUDE_LABEL_IDS),
    GPS_LATITUDE(ExifIfdType.GPS, 0x0002, R.string.gps_latitude, null, null, null),
    GPS_LONGITUDE_REF(ExifIfdType.GPS, 0x0003, R.string.gps_longitude_hemisphere, null, null, ExifLabels.GPS_LONGITUDE_LABEL_IDS),
    GPS_LONGITUDE(ExifIfdType.GPS, 0x0004, R.string.gps_longitude, null, null, null),
    GPS_ALTITUDE_REF(ExifIfdType.GPS, 0x0005, R.string.gps_altitude_reference, null, null, ExifLabels.GPS_ALTITUDE_REF_LABEL_IDS),
    GPS_ALTITUDE(ExifIfdType.GPS, 0x0006, R.string.gps_altitude, null, " m", null),
    GPS_TIMESTAMP(ExifIfdType.GPS, 0x0007, R.string.gps_timestamp, null, null, null),
    GPS_SATELLITES(ExifIfdType.GPS, 0x0008, R.string.gps_satellites, null, null, null),
    GPS_STATUS(ExifIfdType.GPS, 0x0009, R.string.gps_status, null, null, ExifLabels.GPS_STATUS_LABEL_IDS),
    GPS_MEASURE_MODE(ExifIfdType.GPS, 0x000a, R.string.gps_measure_mode, null, null, ExifLabels.GPS_MEASURE_MODE_LABEL_IDS),
    GPS_DOP(ExifIfdType.GPS, 0x000b, R.string.gps_dop, null, null, null),
    GPS_SPEED_REF(ExifIfdType.GPS, 0x000c, R.string.gps_speed_ref, null, null, ExifLabels.GPS_SPEED_REF_LABEL_IDS),
    GPS_SPEED(ExifIfdType.GPS, 0x000d, R.string.gps_speed, null, null, null),
    GPS_TRACK_REF(ExifIfdType.GPS, 0x000e, R.string.gps_track_ref, null, null, ExifLabels.GPS_TRACK_REF_LABEL_IDS),
    GPS_TRACK(ExifIfdType.GPS, 0x000f, R.string.gps_track, null, null, null),
    GPS_IMAGE_DIR_REF(ExifIfdType.GPS, 0x0010, R.string.gps_image_dir_ref, null, null, ExifLabels.GPS_IMAGE_DIR_REF_LABEL_IDS),
    GPS_IMAGE_DIR(ExifIfdType.GPS, 0x0011, R.string.gps_image_direction, null, null, null),
    GPS_MAP_DATUM(ExifIfdType.GPS, 0x0012, R.string.gps_map_datum, null, null, null),
    GPS_DEST_LATITUDE_REF(ExifIfdType.GPS, 0x0013, R.string.gps_dest_latitude_hemisphere, null, null, ExifLabels.GPS_LATITUDE_LABEL_IDS),
    GPS_DEST_LATITUDE(ExifIfdType.GPS, 0x0014, R.string.gps_dest_latitude, null, null, null),
    GPS_DEST_LONGITUDE_REF(ExifIfdType.GPS, 0x0015, R.string.gps_dest_longitude_hemisphere, null, null, ExifLabels.GPS_LONGITUDE_LABEL_IDS),
    GPS_DEST_LONGITUDE(ExifIfdType.GPS, 0x0016, R.string.gps_dest_longitude, null, null, null),
    GPS_DEST_BEARING_REF(ExifIfdType.GPS, 0x0017, R.string.gps_dest_bearing_ref, null, null, ExifLabels.GPS_TRACK_REF_LABEL_IDS),
    GPS_DEST_BEARING(ExifIfdType.GPS, 0x0018, R.string.gps_dest_bearing, null, null, null),
    GPS_DEST_DISTANCE_REF(ExifIfdType.GPS, 0x0019, R.string.gps_dest_distance_ref, null, null, ExifLabels.GPS_DEST_DISTANCE_REF_LABEL_IDS),
    GPS_DEST_DISTANCE(ExifIfdType.GPS, 0x001a, R.string.gps_dest_distance, null, null, null),
    GPS_PROCESSING_METHOD(ExifIfdType.GPS, 0x001b, R.string.gps_processing_method, null, null, null),
    GPS_AREA_INFO(ExifIfdType.GPS, 0x001c, R.string.gps_area_info, null, null, null),
    GPS_DATE_STAMP(ExifIfdType.GPS, 0x001d, R.string.gps_date_stamp, null, null, null),
    GPS_DIFFERENTIAL(ExifIfdType.GPS, 0x001e, R.string.gps_differential, null, null, ExifLabels.GPS_DIFFERENTIAL_LABEL_IDS),
    GPS_HPOS_ERROR(ExifIfdType.GPS, 0x001d, R.string.gps_hpos_error, null, null, null);

    private static final Map<ExifIfdType, Map<Integer, ExifTag>> TAGS_MAP = createTagsMap();

    private final ExifIfdType type;
    private final int code;
    private final int labelId;
    private final String prefix, suffix;
    private final Map<Object, Integer> valueLabelIds;
    private final boolean isBitValues;

    ExifTag(ExifIfdType type, int code, int labelId, String prefix, String suffix, Map<Object, Integer> valueLabelIds) {
        this(type, code, labelId, prefix, suffix, valueLabelIds, false);
    }

    ExifTag(ExifIfdType type, int code, int labelId, String prefix, String suffix,
        Map<Object, Integer> valueLabelIds, boolean isBitValues) {

        this.type = type;
        this.code = code;
        this.labelId = labelId;
        this.prefix = prefix;
        this.suffix = suffix;
        this.valueLabelIds = valueLabelIds;
        this.isBitValues = isBitValues;
    }

    public static ExifTag fromCode(ExifIfdType type, int code) {
        Map<Integer, ExifTag> tags = TAGS_MAP.get(type);
        ExifTag tag = (tags != null) ? tags.get(code) : null;
        return (tag != null) ? tag : UNKNOWN;
    }

    public int getLabelId() {
        return labelId;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public Object translateValue(Context context, Object value) {
        if (valueLabelIds != null) {
            if (isBitValues && (value instanceof Number)) {
                return translateBitValue(context, ((Number) value).intValue(), valueLabelIds);
            }
            else if (value instanceof byte[]) {
                return translateArrayPackedValue(context, (byte[]) value, valueLabelIds);
            }
            else {
                Integer labelId = valueLabelIds.get(value);
                if (labelId != null) {
                    return context.getString(labelId);
                }
            }
        }
        return value;
    }

    public int getSortOrder() {
        int sortOrder = code;
        if (type == ExifIfdType.GPS) {
            sortOrder -= 0x10000;
        }
        return sortOrder;
    }

    public static boolean isIfdIndex(ExifTag tag) {
        return (tag == EXIF_OFFSET) || (tag == INTEROP_OFFSET) || (tag == EXIF_GPS);
    }

    private static Object translateArrayPackedValue(Context context, byte[] byteArray, Map<Object, Integer> codeList) {
        StringBuilder s = new StringBuilder();
        for (byte b: byteArray) {
            Integer labelId = codeList.get((int) b);
            if (labelId != null) {
                if (s.length() > 0) {
                    s.append(", ");
                }
                s.append(context.getString(labelId));
            }
        }
        return s.toString();
    }

    private static Object translateBitValue(Context context, int value, Map<Object, Integer> codeList) {
        StringBuilder s = new StringBuilder();
        for (Map.Entry<Object, Integer> codeEntry: codeList.entrySet()) {
            if (codeEntry.getKey() instanceof Number) {
                int bit = ((Number) codeEntry.getKey()).intValue();
                if ((value & (1 << bit)) > 0) {
                    if (s.length() > 0) {
                        s.append(", ");
                    }
                    s.append(context.getString(codeEntry.getValue()));
                }
            }
        }
        return s.toString();
    }

    private static Map<ExifIfdType, Map<Integer, ExifTag>> createTagsMap() {
        Map<ExifIfdType, Map<Integer, ExifTag>> tagsMap = new HashMap<>();
        for (ExifTag tag: ExifTag.values()) {
            Map<Integer, ExifTag> subMap = tagsMap.computeIfAbsent(tag.type, k -> new HashMap<>());
            subMap.put(tag.code, tag);
        }
        return tagsMap;
    }
}
