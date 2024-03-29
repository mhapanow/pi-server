package mobi.allshoppings.bz.spi.fields;

import java.util.Arrays;


public class StoreBzFields extends BzFields {

	protected StoreBzFields() {
		INTERNAL_ALWAYS_FIELDS.addAll(Arrays.asList(
				"identifier"
				));
		INTERNAL_DEFAULT_FIELDS.addAll(Arrays.asList(
				"identifier",
				"name",
				"avatarId",
				"shoppingId",
				"shoppingName",
				"brandId",
				"brandName"
				));
		INTERNAL_INDEXING_FIELDS.addAll(Arrays.asList(
				"identifier",
				"name",
				"shoppingName",
				"brandName",
				"storeNumber",
				"floorNumber",
				"contactInfo.mail",
				"contactInfo.phone",
				"contactInfo.twitterId",
				"contactInfo.webURL",
				"contactInfo.youtubeChannel",
				"contactInfo.facebookId",
				"contactInfo.googleId"
				));
		INTERNAL_INVALID_FIELDS.addAll(Arrays.asList(
				"key",
				"invoicingInfo*"
				));
		INTERNAL_READONLY_FIELDS.addAll(Arrays.asList(
				"identifier",
				"key",
				"invoicingInfo*"
				));
		INTERNAL_LEVEL_LIST_FIELDS.addAll(Arrays.asList(
				"identifier",
				"name",
				"shoppingId",
				"shoppingName",
				"brandId",
				"brandName",
				"avatarId"
				));
		INTERNAL_LEVEL_ALL_FIELDS.addAll(Arrays.asList(
				"identifier",
				"name",
				"shoppingId",
				"shoppingName",
				"brandId",
				"brandName",
				"avatarId",
				"photoId",
				"videoId",
				"creationDateTime",
				"statusModificationDateTime",
				"contactInfo.mail",
				"contactInfo.phone",
				"contactInfo.twitterId",
				"contactInfo.webURL",
				"contactInfo.youtubeChannel",
				"contactInfo.facebookId",
				"contactInfo.googleId"
				));
		INTERNAL_LEVEL_FAST_FIELDS.addAll(Arrays.asList(
				"identifier",
				"name",
				"avatarId",
				"areaId",
				"shoppingId",
				"shoppingName",
				"brandId",
				"brandName"
				));
		INTERNAL_LEVEL_PUBLIC_FIELDS.addAll(Arrays.asList(
				"identifier",
				"name",
				"shoppingId",
				"shoppingName",
				"brandId",
				"brandName",
				"avatarId",
				"photoId",
				"videoId",
				"creationDateTime",
				"statusModificationDateTime",
				"contactInfo.mail",
				"contactInfo.phone",
				"contactInfo.twitterId",
				"contactInfo.webURL",
				"contactInfo.youtubeChannel",
				"contactInfo.facebookId",
				"contactInfo.googleId"
				));
	}
}