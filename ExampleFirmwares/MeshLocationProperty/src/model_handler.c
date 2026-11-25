/*
 * Copyright (c) 2026 Nordic Semiconductor ASA
 *
 * SPDX-License-Identifier: LicenseRef-Nordic-5-Clause
 */

/*
 * Generic Location Server, Generic Location Setup Server and the four Generic Property Servers
 * (Manufacturer, Admin, User and Client), for testing the nRF Mesh app.
 *
 * Element 1: Generic Location Server and Generic Location Setup Server.
 * Element 2: Generic Manufacturer, Admin, User and Client Property Servers.
 *
 * The values are kept in RAM, so they reset to the values below when the board is reset.
 */

#include <string.h>
#include <zephyr/bluetooth/bluetooth.h>
#include <bluetooth/mesh/models.h>
#include <dk_buttons_and_leds.h>
#include "model_handler.h"

/* The Mesh Model specification v1.0.1 (3.1.7.7) encodes the ground floor 0 as 0xFD. The nRF Connect
 * SDK constant BT_MESH_LOC_FLOOR_NUMBER_GROUND_FLOOR_0 is 0xFC, which is "floor 232 or above" in
 * the specification, so the encoded value is defined here.
 */
#define FLOOR_NUMBER_GROUND_FLOOR_0 0xfd

/* Generic Location. */

static void global_get(struct bt_mesh_loc_srv *srv, struct bt_mesh_msg_ctx *ctx,
		       struct bt_mesh_loc_global *rsp);
static void global_set(struct bt_mesh_loc_srv *srv, struct bt_mesh_msg_ctx *ctx,
		       struct bt_mesh_loc_global *loc);
static void local_get(struct bt_mesh_loc_srv *srv, struct bt_mesh_msg_ctx *ctx,
		      struct bt_mesh_loc_local *rsp);
static void local_set(struct bt_mesh_loc_srv *srv, struct bt_mesh_msg_ctx *ctx,
		      struct bt_mesh_loc_local *loc);

static const struct bt_mesh_loc_srv_handlers loc_handlers = {
	.global_get = global_get,
	.global_set = global_set,
	.local_get = local_get,
	.local_set = local_set,
};

static struct {
	struct bt_mesh_loc_srv srv;
	struct bt_mesh_loc_global global;
	struct bt_mesh_loc_local local;
} loc_ctx = {
	.srv = BT_MESH_LOC_SRV_INIT(&loc_handlers),
	.global = {
		.latitude = 59.9139,
		.longitude = 10.7522,
		.altitude = 23,
	},
	.local = {
		.north = 1250,
		.east = -340,
		.altitude = 45,
		.floor_number = FLOOR_NUMBER_GROUND_FLOOR_0,
		.is_mobile = false,
		.time_delta = 1000,
		.precision_mm = 500,
	},
};

static void global_get(struct bt_mesh_loc_srv *srv, struct bt_mesh_msg_ctx *ctx,
		       struct bt_mesh_loc_global *rsp)
{
	*rsp = loc_ctx.global;
}

static void global_set(struct bt_mesh_loc_srv *srv, struct bt_mesh_msg_ctx *ctx,
		       struct bt_mesh_loc_global *loc)
{
	loc_ctx.global = *loc;
	printk("Global location set\n");
}

static void local_get(struct bt_mesh_loc_srv *srv, struct bt_mesh_msg_ctx *ctx,
		      struct bt_mesh_loc_local *rsp)
{
	*rsp = loc_ctx.local;
}

static void local_set(struct bt_mesh_loc_srv *srv, struct bt_mesh_msg_ctx *ctx,
		      struct bt_mesh_loc_local *loc)
{
	loc_ctx.local = *loc;
	printk("Local location set\n");
}

/* Generic Property. */

#define PROP_VALUE_MAX CONFIG_BT_MESH_PROP_MAXSIZE

/* Device Property IDs from the Mesh Device Properties specification. */
#define PROP_ID_UV_INDEX           0x00B1 /* 1 byte  */
#define PROP_ID_PRESENCE_DETECTED  0x004D /* 1 byte  */
#define PROP_ID_AVERAGE_INPUT_CURRENT 0x0002 /* 3 bytes */
#define PROP_ID_AVERAGE_INPUT_VOLTAGE 0x0003 /* 3 bytes */

struct prop_value {
	uint16_t id;
	uint8_t size;
	uint8_t data[PROP_VALUE_MAX];
};

/* Admin properties can be read and written. Users may read and write the UV index, but only read
 * the presence.
 */
static struct bt_mesh_prop admin_props[] = {
	{ .id = PROP_ID_UV_INDEX, .user_access = BT_MESH_PROP_ACCESS_READ_WRITE },
	{ .id = PROP_ID_PRESENCE_DETECTED, .user_access = BT_MESH_PROP_ACCESS_READ },
};

static struct prop_value admin_values[] = {
	{ .id = PROP_ID_UV_INDEX, .size = 1, .data = { 3 } },
	{ .id = PROP_ID_PRESENCE_DETECTED, .size = 1, .data = { 0 } },
};

/* Manufacturer properties can only be read. */
static struct bt_mesh_prop mfr_props[] = {
	{ .id = PROP_ID_AVERAGE_INPUT_CURRENT, .user_access = BT_MESH_PROP_ACCESS_READ },
	{ .id = PROP_ID_AVERAGE_INPUT_VOLTAGE, .user_access = BT_MESH_PROP_ACCESS_PROHIBITED },
};

static const struct prop_value mfr_values[] = {
	{ .id = PROP_ID_AVERAGE_INPUT_CURRENT, .size = 3, .data = { 0x10, 0x27, 0x00 } },
	{ .id = PROP_ID_AVERAGE_INPUT_VOLTAGE, .size = 3, .data = { 0x20, 0x4E, 0x00 } },
};

/* The Client Property Server lists the properties of a Property Client on the same element. */
static const struct bt_mesh_prop client_props[] = {
	{ .id = PROP_ID_UV_INDEX },
};

static struct prop_value *prop_find(struct prop_value *values, size_t count, uint16_t id)
{
	for (size_t i = 0; i < count; i++) {
		if (values[i].id == id) {
			return &values[i];
		}
	}

	return NULL;
}

static void admin_get(struct bt_mesh_prop_srv *srv, struct bt_mesh_msg_ctx *ctx,
		      struct bt_mesh_prop_val *val)
{
	struct prop_value *prop = prop_find(admin_values, ARRAY_SIZE(admin_values), val->meta.id);

	if (!prop) {
		val->meta.user_access = BT_MESH_PROP_ACCESS_PROHIBITED;
		val->size = 0;
		return;
	}

	val->size = prop->size;
	memcpy(val->value, prop->data, prop->size);
}

static void admin_set(struct bt_mesh_prop_srv *srv, struct bt_mesh_msg_ctx *ctx,
		      struct bt_mesh_prop_val *val)
{
	struct prop_value *prop = prop_find(admin_values, ARRAY_SIZE(admin_values), val->meta.id);

	/* A wrong size, or an unknown property, is reported by prohibiting the id. */
	if (!prop || val->size != prop->size) {
		val->meta.id = BT_MESH_PROP_ID_PROHIBITED;
		return;
	}

	memcpy(prop->data, val->value, prop->size);
	printk("Property 0x%04x set\n", prop->id);
}

static void mfr_get(struct bt_mesh_prop_srv *srv, struct bt_mesh_msg_ctx *ctx,
		    struct bt_mesh_prop_val *val)
{
	for (size_t i = 0; i < ARRAY_SIZE(mfr_values); i++) {
		if (mfr_values[i].id == val->meta.id) {
			val->size = mfr_values[i].size;
			memcpy(val->value, mfr_values[i].data, mfr_values[i].size);
			return;
		}
	}

	val->meta.user_access = BT_MESH_PROP_ACCESS_PROHIBITED;
	val->size = 0;
}

static struct {
	struct bt_mesh_prop_srv mfr;
	struct bt_mesh_prop_srv admin;
	struct bt_mesh_prop_srv user;
	struct bt_mesh_prop_srv client;
} prop_ctx = {
	.mfr = BT_MESH_PROP_SRV_MFR_INIT(mfr_props, mfr_get),
	.admin = BT_MESH_PROP_SRV_ADMIN_INIT(admin_props, admin_get, admin_set),
	.user = BT_MESH_PROP_SRV_USER_INIT(),
	.client = BT_MESH_PROP_SRV_CLIENT_INIT(client_props),
};

/* Blinks the LEDs while attention is requested, so that the node can be found when provisioning. */

static struct k_work_delayable attention_blink_work;
static bool attention;

static void attention_blink(struct k_work *work)
{
	static int idx;

	if (attention) {
		dk_set_leds(BIT(idx++ % 4));
		k_work_reschedule(&attention_blink_work, K_MSEC(30));
	} else {
		dk_set_leds(DK_NO_LEDS_MSK);
	}
}

static void attention_on(const struct bt_mesh_model *mod)
{
	attention = true;
	k_work_reschedule(&attention_blink_work, K_NO_WAIT);
}

static void attention_off(const struct bt_mesh_model *mod)
{
	attention = false;
}

static const struct bt_mesh_health_srv_cb health_srv_cb = {
	.attn_on = attention_on,
	.attn_off = attention_off,
};

static struct bt_mesh_health_srv health_srv = {
	.cb = &health_srv_cb,
};

BT_MESH_HEALTH_PUB_DEFINE(health_pub, 0);

static struct bt_mesh_elem elements[] = {
	BT_MESH_ELEM(
		1, BT_MESH_MODEL_LIST(
			BT_MESH_MODEL_CFG_SRV,
			BT_MESH_MODEL_HEALTH_SRV(&health_srv, &health_pub),
			BT_MESH_MODEL_LOC_SRV(&loc_ctx.srv)),
		BT_MESH_MODEL_NONE),
	BT_MESH_ELEM(
		2, BT_MESH_MODEL_LIST(
			BT_MESH_MODEL_PROP_SRV_USER(&prop_ctx.user),
			BT_MESH_MODEL_PROP_SRV_ADMIN(&prop_ctx.admin),
			BT_MESH_MODEL_PROP_SRV_MFR(&prop_ctx.mfr),
			BT_MESH_MODEL_PROP_SRV_CLIENT(&prop_ctx.client)),
		BT_MESH_MODEL_NONE),
};

static const struct bt_mesh_comp comp = {
	.cid = CONFIG_BT_COMPANY_ID,
	.elem = elements,
	.elem_count = ARRAY_SIZE(elements),
};

const struct bt_mesh_comp *model_handler_init(void)
{
	k_work_init_delayable(&attention_blink_work, attention_blink);

	return &comp;
}
