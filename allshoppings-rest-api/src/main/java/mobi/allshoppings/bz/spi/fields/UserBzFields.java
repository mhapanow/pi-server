package mobi.allshoppings.bz.spi.fields;

import java.util.Arrays;


public class UserBzFields extends BzFields {

	protected UserBzFields() {
		INTERNAL_ALWAYS_FIELDS.addAll(Arrays.asList(
				"identifier"
				));
		INTERNAL_DEFAULT_FIELDS.addAll(Arrays.asList(
				"identifier",
				"email"
				));
		INTERNAL_INVALID_FIELDS.addAll(Arrays.asList(
				"key",
				"securitySettings*"
				));
		INTERNAL_READONLY_FIELDS.addAll(Arrays.asList(
				"identifier",
				"key",
				"securitySettings*"
				));
		INTERNAL_LEVEL_LIST_FIELDS.addAll(Arrays.asList(
				"identifier",
				"fullname",
				"trackingCode",
				"email",
				"avatarId",
				"firstname",
				"lastname",
				"birthDate",
				"points"
				));
		INTERNAL_LEVEL_ALL_FIELDS.addAll(Arrays.asList(
				"identifier",
				"avatarId",
				"birthDate",
				"firstname",
				"lastname",
				"fullname",
				"fullnameReverse",
				"trackingCode",
				"email",
				"lastLogin",
				"gender",
				"creationDateTime",
				"receivePushMessages",
				"geoFenceEnabled",
				"viewLocation.country",
				"securitySettings.authToken",
				"securitySettings.authTokenValidity",
				"securitySettings.status",
				"securitySettings.role",
				"address.city",
				"address.country",
				"address.latitude",
				"address.longitude",
				"address.neighborhood",
				"address.province",
				"address.streetName",
				"address.streetNumber",
				"address.zipCode",
				"contactInfo.facebookId",
				"contactInfo.googleId",
				"contactInfo.mobile",
				"contactInfo.phone",
				"contactInfo.twitterId",
				"contactInfo.webURL",
				"contactInfo.youtubeChannel",
				"statusModificationDateTime",
				"points"
				));
		INTERNAL_LEVEL_FAST_FIELDS.addAll(Arrays.asList(
				"identifier",
				"fullname",
				"trackingCode",
				"email",
				"firstname",
				"lastname",
				"points"
				));
		INTERNAL_INDEXING_FIELDS.addAll(Arrays.asList(
				"identifier",
				"birthDate",
				"firstname",
				"lastname",
				"email",
				"gender",
				"viewLocation.country",
				"address.city",
				"address.country",
				"address.neighborhood",
				"address.province",
				"address.streetName",
				"address.streetNumber",
				"address.zipCode",
				"contactInfo.facebookId",
				"contactInfo.googleId",
				"contactInfo.mobile",
				"contactInfo.phone",
				"contactInfo.twitterId",
				"contactInfo.webURL",
				"contactInfo.youtubeChannel",
				"points"
				));
		INTERNAL_LEVEL_PUBLIC_FIELDS.addAll(Arrays.asList(
				"fullname",
				"firstname",
				"lastname",
				"avatarId",
				"points"
				));
	}
}