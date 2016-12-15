package mobi.allshoppings.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.springframework.util.StringUtils;

import com.inodes.datanucleus.model.Key;

import mobi.allshoppings.model.interfaces.ACLAware;
import mobi.allshoppings.model.interfaces.Identificable;
import mobi.allshoppings.model.interfaces.Indexable;
import mobi.allshoppings.model.interfaces.ModelKey;
import mobi.allshoppings.model.interfaces.Replicable;
import mobi.allshoppings.model.interfaces.StatusAware;
import mobi.allshoppings.model.interfaces.ViewLocationAware;
import mobi.allshoppings.model.tools.ACL;
import mobi.allshoppings.model.tools.ViewLocation;

@SuppressWarnings("serial")
@PersistenceCapable(detachable="true")
public class Shopping implements ModelKey, Serializable, Identificable, Indexable, ViewLocationAware, ACLAware, StatusAware, Replicable {

	@PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.UNSPECIFIED)
    private Key key;

	private String name;
	private String description;
	private String avatarId;
	private String frontImageId;
	
	@Persistent(defaultFetchGroup = "true")
	private List<String> photoId;
	
	@Persistent(defaultFetchGroup = "true")
	private List<String> videoId;
	
	private Date creationDateTime;
	private Date statusModificationDateTime;
	private Date lastUpdate;
	private Integer fenceSize;
	private Integer checkinAreaSize;
	private Float timezone;
	private String customCheckinMessage;
	
	@Persistent(defaultFetchGroup = "true")
	@Embedded
	private Address address;

	@Persistent(defaultFetchGroup = "true")
	@Embedded
	private ContactInfo contactInfo;

	@Persistent(defaultFetchGroup = "true")
	@Embedded
	private StructureInfo structureInfo;

	@Persistent(defaultFetchGroup = "true")
	@Embedded
	private InvoicingInfo invoicingInfo;

	@Persistent(defaultFetchGroup = "true")
	private List<String> serviceId;
	
	@Persistent(defaultFetchGroup = "true")
	@Embedded
	private ACL acl;
	
	private Integer status;

	private String source;
	
	// Search fields ... this is too ugly... Fuck you Google!!!!
	@SuppressWarnings("unused")
	private String uIdentifier;
	@SuppressWarnings("unused")
	private String uProvince;
	@SuppressWarnings("unused")
	private String uCountry;
	@SuppressWarnings("unused")
	private String uName;

	@NotPersistent
	private boolean doIndexNow = true;
	
    public Shopping() {
		this.creationDateTime = new Date();
		this.contactInfo = new ContactInfo();
		this.structureInfo = new StructureInfo();
		this.address = new Address();
		this.photoId = new ArrayList<String>();
		this.videoId = new ArrayList<String>();
		this.serviceId = new ArrayList<String>();
		this.acl = new ACL();
		this.status = StatusAware.STATUS_ENABLED;
		this.timezone = 0F;
    }

	/**
	 * @return the key
	 */
	public Key getKey() {
		return key;
	}

	/**
	 * @param key the key to set
	 */
	public void setKey(Key key) {
		this.key = key;
	}

	/**
	 * @return this entity key
	 */
	public String getIdentifier() {
		return this.getKey() != null ? this.getKey().getName() : "";
	}

	/**
	 * Pre store information to assign index values
	 */
	@Override
	public void preStore() {
		uIdentifier = getIdentifier().toUpperCase();
		uProvince = getAddress().getProvince() == null ? "" : getAddress().getProvince().toUpperCase();
		uCountry = getAddress().getCountry() == null ? "" : getAddress().getCountry().toUpperCase();
		uName = getName() == null ? "" : getName().toUpperCase();
		this.lastUpdate = new Date();
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the avatarId
	 */
	public String getAvatarId() {
		return avatarId;
	}

	/**
	 * @param avatarId the avatarId to set
	 */
	public void setAvatarId(String avatarId) {
		this.avatarId = avatarId;
	}

	/**
	 * @return the frontImageId
	 */
	public String getFrontImageId() {
		return frontImageId;
	}

	/**
	 * @param frontImageId the frontImageId to set
	 */
	public void setFrontImageId(String frontImageId) {
		this.frontImageId = frontImageId;
	}

	/**
	 * @return the photoId
	 */
	public List<String> getPhotoId() {
		return photoId;
	}

	/**
	 * @param photoId the photoId to set
	 */
	public void setPhotoId(List<String> photoId) {
		this.photoId = photoId;
	}

	/**
	 * Adds an Image to the current photo Id array
	 * @param photoId the Image Id to add
	 */
	public void addPhotoId(String photoId) {
		if( !this.photoId.contains(photoId)) {
			this.photoId.add(photoId);
		}
	}
	
	/**
	 * Removes an Image from the current photo id array
	 * @param photoId the Image Id to remove
	 */
	public void removePhotoId(String photoId) {
		if( this.photoId.contains(photoId)) {
			this.photoId.remove(photoId);
		}
	}
	
	/**
	 * @return the videoId
	 */
	public List<String> getVideoId() {
		return videoId;
	}

	/**
	 * @param videoId the videoId to set
	 */
	public void setVideoId(List<String> videoId) {
		this.videoId = videoId;
	}

	/**
	 * @return the creationDateTime
	 */
	public Date getCreationDateTime() {
		return creationDateTime;
	}

	/**
	 * @param creationDateTime the creationDateTime to set
	 */
	public void setCreationDateTime(Date creationDateTime) {
		this.creationDateTime = creationDateTime;
	}

	/**
	 * @return the statusModificationDateTime
	 */
	public Date getStatusModificationDateTime() {
		return statusModificationDateTime;
	}

	/**
	 * @param statusModificationDateTime the statusModificationDateTime to set
	 */
	public void setStatusModificationDateTime(Date statusModificationDateTime) {
		this.statusModificationDateTime = statusModificationDateTime;
	}

	/**
	 * @return the address
	 */
	public Address getAddress() {
		return address;
	}

	/**
	 * @param address the address to set
	 */
	public void setAddress(Address address) {
		this.address = address;
	}

	/**
	 * @return the contactInfo
	 */
	public ContactInfo getContactInfo() {
		return contactInfo;
	}

	/**
	 * @param contactInfo the contactInfo to set
	 */
	public void setContactInfo(ContactInfo contactInfo) {
		this.contactInfo = contactInfo;
	}

	/**
	 * @return the structureInfo
	 */
	public StructureInfo getStructureInfo() {
		return structureInfo;
	}

	/**
	 * @param structureInfo the structureInfo to set
	 */
	public void setStructureInfo(StructureInfo structureInfo) {
		this.structureInfo = structureInfo;
	}

	/**
	 * @return the invoicingInfo
	 */
	public InvoicingInfo getInvoicingInfo() {
		return invoicingInfo;
	}

	/**
	 * @param invoicingInfo the invoicingInfo to set
	 */
	public void setInvoicingInfo(InvoicingInfo invoicingInfo) {
		this.invoicingInfo = invoicingInfo;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Shopping other = (Shopping) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	/**
	 * @return the serviceId
	 */
	public List<String> getServiceId() {
		return serviceId;
	}

	/**
	 * @param serviceId the serviceId to set
	 */
	public void setServiceId(List<String> serviceId) {
		this.serviceId = serviceId;
	}

	/**
	 * Adds a new service to the services id array
	 * @param serviceId The service to add
	 */
	public void addServiceId(String serviceId) {
		if(!this.serviceId.contains(serviceId)) {
			this.serviceId.add(serviceId);
		}
	}
	
	/**
	 * Clears the services id array
	 */
	public void clearServices() {
		this.serviceId.clear();
	}
	
	/**
	 * Removes an service from the services id array
	 * @param serviceId The service to remove
	 */
	public void removeServiceId(String serviceId) {
		if(this.serviceId.contains(serviceId)) {
			this.serviceId.remove(serviceId);
		}
	}

	/**
	 * @return the lastUpdate
	 */
	public Date getLastUpdate() {
		return lastUpdate;
	}

	/**
	 * @param lastUpdate the lastUpdate to set
	 */
	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Shopping [name=" + name + "]";
	}

	/**
	 * @return the fenceSize
	 */
	public Integer getFenceSize() {
		return fenceSize;
	}

	/**
	 * @param fenceSize the fenceSize to set
	 */
	public void setFenceSize(Integer fenceSize) {
		this.fenceSize = fenceSize;
	}

	@Override
	public boolean isAvailableFor(ViewLocation vl) {
		if( null == vl || !StringUtils.hasText(vl.getCountry()) 
				|| null == address || !StringUtils.hasText(address.getCountry())) return true;
		if( vl.getCountry().equalsIgnoreCase(address.getCountry())) return true;
		return false;
	}

	@Override
	public ACL getAcl() {
		return acl;
	}

	@Override
	public boolean doIndex() {
		return doIndexNow;
	}

	@Override
	public void disableIndexing(boolean val) {
		this.doIndexNow = !val;
	}

	/**
	 * @return the status
	 */
	public Integer getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(Integer status) {
		this.status = status;
	}

	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * @return the checkinAreaSize
	 */
	public Integer getCheckinAreaSize() {
		return checkinAreaSize;
	}

	/**
	 * @param checkinAreaSize the checkinAreaSize to set
	 */
	public void setCheckinAreaSize(Integer checkingAreaSize) {
		this.checkinAreaSize = checkingAreaSize;
	}

	/**
	 * Returns current country
	 */
	public String getCountry() {
		return this.address != null ? this.address.getCountry() : null;
	}

	/**
	 * @return the timezone
	 */
	public Float getTimezone() {
		return timezone;
	}

	/**
	 * @param timezone the timezone to set
	 */
	public void setTimezone(Float timezone) {
		this.timezone = timezone;
	}

	/**
	 * @return the customCheckinMessage
	 */
	public String getCustomCheckinMessage() {
		return customCheckinMessage;
	}

	/**
	 * @param customCheckinMessage the customCheckinMessage to set
	 */
	public void setCustomCheckinMessage(String customCheckinMessage) {
		this.customCheckinMessage = customCheckinMessage;
	}
	
}
