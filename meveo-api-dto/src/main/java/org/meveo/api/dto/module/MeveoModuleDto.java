package org.meveo.api.dto.module;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.StringUtils;
import org.meveo.api.dto.BaseEntityDto;
import org.meveo.api.dto.ScriptInstanceDto;
import org.meveo.model.module.MeveoModule;
import org.meveo.model.module.ModuleLicenseEnum;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The Class MeveoModuleDto.
 * 
 * @author andrius
 */
@XmlRootElement(name = "Module")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeveoModuleDto extends BaseDataModelDto {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** The license. */
    @XmlAttribute(required = true)
    private ModuleLicenseEnum license;

    /** The logo picture. */
    private String logoPicture;
    
    /** The logo picture file. */
    private byte[] logoPictureFile;

    /** The script. */
    private ScriptInstanceDto script;

    /** The module items. */
    private List<MeveoModuleItemDto> moduleItems;

    /**
     * Instantiates a new meveo module dto.
     */
    public MeveoModuleDto() {
    }

    /**
     * Instantiates a new meveo module dto.
     *
     * @param meveoModule the meveo module
     */
    public MeveoModuleDto(MeveoModule meveoModule) {
        super(meveoModule);
        this.license = meveoModule.getLicense();
        this.logoPicture = meveoModule.getLogoPicture();
        this.moduleItems = new ArrayList<>();
        if (meveoModule.getScript() != null) {
            this.setScript(new ScriptInstanceDto(meveoModule.getScript()));
        }
    }

    /**
     * Gets the license.
     *
     * @return the license
     */
    public ModuleLicenseEnum getLicense() {
        return license;
    }

    /**
     * Sets the license.
     *
     * @param license the new license
     */
    public void setLicense(ModuleLicenseEnum license) {
        this.license = license;
    }

    /**
     * Gets the logo picture.
     *
     * @return the logo picture
     */
    public String getLogoPicture() {
        return logoPicture;
    }

    /**
     * Sets the logo picture.
     *
     * @param logoPicture the new logo picture
     */
    public void setLogoPicture(String logoPicture) {
        this.logoPicture = logoPicture;
    }

    /**
     * Gets the logo picture file.
     *
     * @return the logo picture file
     */
    public byte[] getLogoPictureFile() {
        return logoPictureFile;
    }

    /**
     * Sets the logo picture file.
     *
     * @param logoPictureFile the new logo picture file
     */
    public void setLogoPictureFile(byte[] logoPictureFile) {
        this.logoPictureFile = logoPictureFile;
    }

    /**
     * Gets the module items.
     *
     * @return the module items
     */
    public List<MeveoModuleItemDto> getModuleItems() {
        return moduleItems;
    }

    /**
     * Sets the module items.
     *
     * @param moduleItems the new module items
     */
    public void setModuleItems(List<MeveoModuleItemDto> moduleItems) {
        this.moduleItems = moduleItems;
    }

    /**
     * Adds the module item.
     *
     * @param item the item
     */
    public void addModuleItem(BaseEntityDto item) {
    	MeveoModuleItemDto meveoModuleItemDto = new MeveoModuleItemDto(item.getClass().getName(), item);
        if (!moduleItems.contains(meveoModuleItemDto)) {
            moduleItems.add(meveoModuleItemDto);
        }
    }

    /* (non-Javadoc)
     * @see org.meveo.model.IEntity#isTransient()
     */
    @Override
    public boolean isTransient() {
        return true;
    }

    /**
     * Gets the script.
     *
     * @return the script
     */
    public ScriptInstanceDto getScript() {
        return script;
    }

    /**
     * Sets the script.
     *
     * @param script the new script
     */
    public void setScript(ScriptInstanceDto script) {
        this.script = script;
    }

    /**
     * Checks if is code only.
     *
     * @return true, if is code only
     */
    public boolean isCodeOnly() {
        return StringUtils.isBlank(getDescription()) && license == null && StringUtils.isBlank(logoPicture) && logoPictureFile == null && script == null
                && (moduleItems == null || moduleItems.isEmpty());
    }
    
    @Override
    public String toString() {
        final int maxLen = 10;
        return String.format("ModuleDto [code=%s, license=%s, description=%s, logoPicture=%s, logoPictureFile=%s, moduleItems=%s, script=%s]", getCode(), license, getDescription(),
            logoPicture, logoPictureFile, moduleItems != null ? moduleItems.subList(0, Math.min(moduleItems.size(), maxLen)) : null, script);
    }
}