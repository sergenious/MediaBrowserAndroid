package com.sergenious.mediabrowser.io.exif;

import com.sergenious.mediabrowser.R;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExifLabels {
    public static final Map<Object, Integer> COMPONENT_CONFIGURATION_LABELS = new HashMap<Object, Integer>() {{
        put(1, R.string.component_y);
        put(2, R.string.component_cb);
        put(3, R.string.component_cr);
        put(4, R.string.component_r);
        put(5, R.string.component_g);
        put(6, R.string.component_b);
    }};

    public static final Map<Object, Integer> SUBFILE_TYPE_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(0x0, R.string.full_resolution_image);
        put(0x1, R.string.reduced_resolution_image);
        put(0x2, R.string.single_page_of_multi);
        put(0x3, R.string.single_page_of_multi_reduced);
        put(0x4, R.string.transparency_mask);
        put(0x5, R.string.transparency_mask_of_reduced_res_image);
        put(0x6, R.string.transparency_mask_of_multi_page_image);
        put(0x7, R.string.transparency_mask_of_reduced_multi_image);
        put(0x8, R.string.depth_map);
        put(0x9, R.string.depth_map_of_reduced_res_image);
        put(0x10, R.string.enhanced_image_data);
        put(0x10001, R.string.alternate_reduced_res_image);
        put(0x10004, R.string.semantic_mask);
        put(0xffffffff, R.string.invalid);
    }};

    public static final Map<Object, Integer> COMPRESSION_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(1, R.string.uncompressed);
        put(2, R.string.ccitt_1d);
        put(3, R.string.t4_group_3_fax);
        put(4, R.string.t6_group_4_fax);
        put(5, R.string.lzw);
        put(6, R.string.jpeg_old_style);
        put(7, R.string.jpeg);
        put(8, R.string.adobe_deflate);
        put(9, R.string.jbig_b_w);
        put(10, R.string.jbig_color);
        put(99, R.string.jpeg);
    }};

    public static final Map<Object, Integer> PHOTOMETRIC_INTERPRETATION_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(0, R.string.white_is_zero);
        put(1, R.string.black_is_zero);
        put(2, R.string.rgb);
        put(3, R.string.rgb_palette);
        put(4, R.string.transparency_mask);
        put(5, R.string.cmyk);
        put(6, R.string.y_cb_cr);
        put(8, R.string.cielab);
        put(9, R.string.icc_lab);
        put(10, R.string.itu_lab);
        put(32803, R.string.color_filter_array);
        put(32844, R.string.pixar_log_l);
        put(32845, R.string.pixar_log_luv);
        put(32892, R.string.sequential_color_filter);
        put(34892, R.string.linear_raw);
        put(51177, R.string.depth_map);
        put(52527, R.string.semantic_mask);
    }};

    public static final Map<Object, Integer> THRESHOLDING_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(1, R.string.no_dither_or_halftone);
        put(2, R.string.ordered_dither_or_halfton);
        put(3, R.string.randomized_dither);
    }};

    public static final Map<Object, Integer> FILL_ORDER_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(1, R.string.normal);
        put(2, R.string.reversed);
    }};

    public static final Map<Object, Integer> ORIENTATION_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(1, R.string.horizontal);
        put(2, R.string.mirror_horizontal);
        put(3, R.string.rotate_180);
        put(4, R.string.mirror_vertical);
        put(5, R.string.mirror_horizontal_rotate_270);
        put(6, R.string.rotate_90);
        put(7, R.string.mirror_horizontal_rotate_90);
        put(8, R.string.rotate_270);
    }};

    public static final Map<Object, Integer> GRAY_RESPONSE_UNIT_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(1, R.string.number0_1);
        put(2, R.string.number0_001);
        put(3, R.string.number0_0001);
        put(4, R.string.number1e_minus5);
        put(5, R.string.number1e_minus6);
    }};

    public static final Map<Object, Integer> RESOLUTION_UNIT_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(1, R.string.none);
        put(2, R.string.inches);
        put(3, R.string.centimeters);
    }};

    public static final Map<Object, Integer> PLANAR_CONFIGURATION_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(1, R.string.chunky);
        put(2, R.string.planar);
    }};

    public static final Map<Object, Integer> CODING_METHODS_LABEL_IDS = new LinkedHashMap<Object, Integer>() {{
        // note these are bit indices here
        put(0, R.string.unspecified_compression);
        put(1, R.string.modified_huffman);
        put(2, R.string.modified_read);
        put(3, R.string.modified_mr);
        put(4, R.string.jbig);
        put(5, R.string.baseline_jpeg);
        put(6, R.string.jbig_color);
    }};

    public static final Map<Object, Integer> Y_CB_CR_POSITIONING_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(1, R.string.centered);
        put(2, R.string.co_sited);
    }};

    public static final Map<Object, Integer> EXPOSURE_PROGRAM_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(0, R.string.not_defined);
        put(1, R.string.manual);
        put(2, R.string.program_ae);
        put(3, R.string.aperture_priority_ae);
        put(4, R.string.shutter_speed_priority_ae);
        put(5, R.string.creative_slow_speed);
        put(6, R.string.action_high_speed);
        put(7, R.string.portrait);
        put(8, R.string.landscape);
        put(9, R.string.bulb);
    }};

    public static final Map<Object, Integer> SENSIVITY_TYPE_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(0, R.string.unknown);
        put(1, R.string.standard_output_sensitivity);
        put(2, R.string.recommended_exposure_index);
        put(3, R.string.iso_speed);// = ISO Speed
        put(4, R.string.standard_output_sens_and_rei);
        put(5, R.string.standard_output_sens_and_iso_speed);
        put(6, R.string.recommended_exposure_inedx_and_iso_speed);
        put(7, R.string.standard_output_sens_rei_and_iso_speed);
    }};

    public static final Map<Object, Integer> METERING_MODE_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(0, R.string.unknown);
        put(1, R.string.average);
        put(2, R.string.center_weighted_average);
        put(3, R.string.spot);
        put(4, R.string.multi_spot);
        put(5, R.string.multi_segment);
        put(6, R.string.partial);
        put(255, R.string.other);
    }};

    public static final Map<Object, Integer> LIGHT_SOURCE_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(0, R.string.unknown);
        put(1, R.string.daylight);
        put(2, R.string.fluorescent);
        put(3, R.string.tungsten_incadescent);
        put(4, R.string.flash);
        put(9, R.string.fine_weather);
        put(10, R.string.cloudy);
        put(11, R.string.shade);
        put(12, R.string.daylight_fluorescent);
        put(13, R.string.day_white_fluorescent);
        put(14, R.string.cool_white_fluorescent);
        put(15, R.string.white_fluorescent);
        put(16, R.string.warm_white_fluorescent);
        put(17, R.string.standard_light_a);
        put(18, R.string.standard_light_b);
        put(19, R.string.standard_light_c);
        put(20, R.string.light_d55);
        put(21, R.string.light_d65);
        put(22, R.string.light_d75);
        put(23, R.string.light_d50);
        put(24, R.string.iso_studio_tungsten);
        put(255, R.string.other);
    }};

    public static final Map<Object, Integer> FLASH_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(0x0, R.string.no_flash); // No Flash
        put(0x1, R.string.flash_fired); // Fired
        put(0x5, R.string.flash_fired_return_not_detected); // Fired, Return not detected
        put(0x7, R.string.flash_fired_return_detected); // Fired, Return detected
        put(0x8, R.string.flash_on_not_fired); // On, Did not fire
        put(0x9, R.string.flash_on_fired); // On, Fired
        put(0xd, R.string.flash_on_return_not_detected); // On, Return not detected
        put(0xf, R.string.flash_on_return_detected); // On, Return detected
        put(0x10, R.string.flash_off_not_fired); // Off, Did not fire
        put(0x14, R.string.flash_off_not_fired_return_not_detected); // Off, Did not fire, Return not detected
        put(0x18, R.string.flash_auto_not_fired); // Auto, Did not fire
        put(0x19, R.string.flash_auto_fired); // Auto, Fired
        put(0x1d, R.string.flash_auto_fired_return_not_detected); // Auto, Fired, Return not detected
        put(0x1f, R.string.flash_auto_fired_return_detected); // Auto, Fired, Return detected
        put(0x20, R.string.flash_no_function); // No flash function
        put(0x30, R.string.flash_off_no_function); // Off, No flash function
        put(0x41, R.string.flash_fired_red_eye_reduction); // Fired, Red-eye reduction
        put(0x45, R.string.flash_fired_red_eye_ret_not_detected); // Fired, Red-eye reduction, Return not detected
        put(0x47, R.string.flash_fired_red_eye_return_detected); // Fired, Red-eye reduction, Return detected
        put(0x49, R.string.flash_on_red_eye_reduction); // On, Red-eye reduction
        put(0x4d, R.string.flash_on_red_eye_ret_not_detected); // On, Red-eye reduction, Return not detected
        put(0x4f, R.string.flash_on_red_eye_return_detected); // On, Red-eye reduction, Return detected
        put(0x50, R.string.flash_off_red_eye_reduction); // Off, Red-eye reduction
        put(0x58, R.string.flash_auto_not_fired_red_eye); // Auto, Did not fire, Red-eye reduction
        put(0x59, R.string.flash_auto_fired_red_eye_reduction); // Auto, Fired, Red-eye reduction
        put(0x5d, R.string.flash_auto_fired_red_eye_ret_not_detected); // Auto, Fired, Red-eye reduction, Return not detected
        put(0x5f, R.string.flash_auto_fired_red_eye_return_detected); // Auto, Fired, Red-eye reduction, Return detected
    }};

    public static final Map<Object, Integer> SENSING_MODE_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(1, R.string.monochrome_area);
        put(2, R.string.one_chip_color_area);
        put(3, R.string.two_chip_color_area);
        put(4, R.string.three_chip_color_area);
        put(5, R.string.color_sequential_area);
        put(6, R.string.monochrome_linear);
        put(7, R.string.trilinear);
        put(8, R.string.color_sequential_linear);
    }};

    public static final Map<Object, Integer> FILE_SOURCE_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(1, R.string.film_scanner);
        put(2, R.string.reflection_print_scanner);
        put(3, R.string.digital_camera);
    }};

    public static final Map<Object, Integer> SCENE_TYPE_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(1, R.string.directly_photographed);
    }};

    public static final Map<Object, Integer> COLOR_SPACE_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(0x1, R.string.srgb);
        put(0x2, R.string.adobe_rgb);
        put(0xfffd, R.string.wide_gamut_rgb);
        put(0xfffe, R.string.icc_profile);
        put(0xffff, R.string.uncalibrated);
    }};

    public static final Map<Object, Integer> FOCAL_PLANE_RESOLUTION_UNIT_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(1, R.string.none);
        put(2, R.string.inches);
        put(3, R.string.centimeters);
        put(4, R.string.millimeters);
        put(5, R.string.micrometers);
    }};

    public static final Map<Object, Integer> SECURITY_CLASSIFICATION_LABEL_IDS = new HashMap<Object, Integer>() {{
        put("C", R.string.confidential);
        put("R", R.string.restricted);
        put("S", R.string.secret);
        put("T", R.string.top_secret);
        put("U", R.string.unclassified);
    }};

    public static final Map<Object, Integer> CUSTOM_RENDERED_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(0, R.string.normal);
        put(1, R.string.custom);
        put(2, R.string.hdr_no_original_saved);
        put(3, R.string.hdr_original_saved);
        put(4, R.string.original_for_hdr);
        put(6, R.string.panorama);
        put(7, R.string.portrait_hdr);
        put(8, R.string.portrait);
    }};

    public static final Map<Object, Integer> EXPOSURE_MODE_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(0, R.string.auto);
        put(1, R.string.manual);
        put(2, R.string.auto_bracket);
    }};

    public static final Map<Object, Integer> WHITE_BALANCE_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(0, R.string.auto);
        put(1, R.string.manual);
    }};

    public static final Map<Object, Integer> SCENE_CAPTURE_TYPE_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(0, R.string.standard);
        put(1, R.string.landscape);
        put(2, R.string.portrait);
        put(3, R.string.night);
        put(4, R.string.other);
    }};

    public static final Map<Object, Integer> GAIN_CONTROL_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(0, R.string.none);
        put(1, R.string.low_gain_up);
        put(2, R.string.high_gain_up);
        put(3, R.string.low_gain_down);
        put(4, R.string.high_gain_down);
    }};

    public static final Map<Object, Integer> CONTRAST_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(0, R.string.normal);
        put(1, R.string.low);
        put(2, R.string.high);
    }};

    public static final Map<Object, Integer> SATURATION_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(0, R.string.normal);
        put(1, R.string.low);
        put(2, R.string.high);
    }};

    public static final Map<Object, Integer> SHARPNESS_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(0, R.string.normal);
        put(1, R.string.soft);
        put(2, R.string.hard);
    }};

    public static final Map<Object, Integer> SUBJECT_DISTANCE_RANGE_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(0, R.string.unknown);
        put(1, R.string.macro);
        put(2, R.string.distance_close);
        put(3, R.string.distant);
    }};

    public static final Map<Object, Integer> GPS_LATITUDE_LABEL_IDS = new HashMap<Object, Integer>() {{
        put("N", R.string.north);
        put("S", R.string.south);
    }};

    public static final Map<Object, Integer> GPS_LONGITUDE_LABEL_IDS = new HashMap<Object, Integer>() {{
        put("E", R.string.east);
        put("W", R.string.west);
    }};

    public static final Map<Object, Integer> GPS_ALTITUDE_REF_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(0, R.string.above_sea_level);
        put(1, R.string.below_sea_level);
    }};

    public static final Map<Object, Integer> GPS_STATUS_LABEL_IDS = new HashMap<Object, Integer>() {{
        put("A", R.string.measurement_active);
        put("V", R.string.measurement_void);
    }};

    public static final Map<Object, Integer> GPS_MEASURE_MODE_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(2, R.string._2d_measurement);
        put(3, R.string._3d_measurement);
    }};

    public static final Map<Object, Integer> GPS_SPEED_REF_LABEL_IDS = new HashMap<Object, Integer>() {{
        put("K", R.string.kmh);
        put("M", R.string.mph);
        put("N", R.string.knots);
    }};

    public static final Map<Object, Integer> GPS_TRACK_REF_LABEL_IDS = new HashMap<Object, Integer>() {{
        put("M", R.string.magnetic_north);
        put("T", R.string.true_north);
    }};

    public static final Map<Object, Integer> GPS_IMAGE_DIR_REF_LABEL_IDS = new HashMap<Object, Integer>() {{
        put("M", R.string.magnetic_north);
        put("T", R.string.true_north);
    }};

    public static final Map<Object, Integer> GPS_DEST_DISTANCE_REF_LABEL_IDS = new HashMap<Object, Integer>() {{
        put("K", R.string.kilometers);
        put("M", R.string.miles);
        put("N", R.string.nautical_miles);
    }};

    public static final Map<Object, Integer> GPS_DIFFERENTIAL_LABEL_IDS = new HashMap<Object, Integer>() {{
        put(0, R.string.no_correction);
        put(1, R.string.differential_corrected);
    }};
}
